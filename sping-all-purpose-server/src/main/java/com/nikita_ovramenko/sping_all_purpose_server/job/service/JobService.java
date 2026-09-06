package com.nikita_ovramenko.sping_all_purpose_server.job.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.service.ClientResolver;
import com.nikita_ovramenko.sping_all_purpose_server.common.dto.LineTotals;
import com.nikita_ovramenko.sping_all_purpose_server.common.dto.PageResponse;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.BadRequestException;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.Specs;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobResponse;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobSummary;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.job.exception.JobNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.job.mapper.JobMapper;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;
import com.nikita_ovramenko.sping_all_purpose_server.job.repository.JobRepo;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository.JobLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.service.LocationResolver;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.exception.QuoteNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * Scheduling and tracking work.
 *
 * <p>A job either comes from an accepted quote or is walk-in work that never had one.
 * Both end up in the same shape; only how client, location and lines are obtained
 * differs.
 */
@Service
public class JobService {

    private final JobRepo jobRepo;
    private final JobLineItemRepo lineItemRepo;
    private final QuoteRepo quoteRepo;
    private final OrganizationLookup organizationLookup;
    private final ClientResolver clientResolver;
    private final LocationResolver locationResolver;
    private final OfferedServiceResolver offeredServiceResolver;
    private final JobMapper jobMapper;

    public JobService(JobRepo jobRepo, JobLineItemRepo lineItemRepo, QuoteRepo quoteRepo,
            OrganizationLookup organizationLookup, ClientResolver clientResolver,
            LocationResolver locationResolver, OfferedServiceResolver offeredServiceResolver,
            JobMapper jobMapper) {
        this.jobRepo = jobRepo;
        this.lineItemRepo = lineItemRepo;
        this.quoteRepo = quoteRepo;
        this.organizationLookup = organizationLookup;
        this.clientResolver = clientResolver;
        this.locationResolver = locationResolver;
        this.offeredServiceResolver = offeredServiceResolver;
        this.jobMapper = jobMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> list(String organizationSlug, JobStatus status,
            String clientEmail, Pageable pageable) {

        Specification<Job> spec = Specs.allOfNonNull(
                JobSpecifications.fetchSummaryAssociations(),
                JobSpecifications.organizationSlug(organizationSlug),
                JobSpecifications.status(status),
                JobSpecifications.clientEmail(clientEmail));

        Page<Job> page = jobRepo.findAll(spec, pageable);
        Map<Long, LineTotals> totals = loadTotals(page.getContent());

        return PageResponse.of(page, job -> toSummary(job, totals.get(job.getId())));
    }

    @Transactional(readOnly = true)
    public JobResponse get(Long id) {
        return jobMapper.toResponse(require(id));
    }

    @Transactional
    public JobResponse create(JobCreateRequest request) {
        return jobMapper.toResponse(
                request.fromQuote() ? createFromQuote(request) : createWalkIn(request));
    }

    /** Partial update: a null field is left unchanged. */
    @Transactional
    public JobResponse update(Long id, JobUpdateRequest request) {
        Job job = require(id);

        if (request.status() != null) {
            job.setStatus(request.status());
        }
        if (request.scheduledAt() != null) {
            job.setScheduledAt(request.scheduledAt());
        }
        if (request.startedAt() != null) {
            job.setStartedAt(request.startedAt());
        }
        if (request.completedAt() != null) {
            job.setCompletedAt(request.completedAt());
        }
        if (request.description() != null) {
            job.setDescription(request.description());
        }

        // saveAndFlush, not save: @LastModifiedDate is applied by the auditing listener
        // on @PreUpdate, which only fires at flush. Without forcing it the response
        // would carry the pre-update timestamp while the database gets the new one.
        return jobMapper.toResponse(jobRepo.saveAndFlush(job));
    }

    /**
     * The accepted-quote-becomes-work step: copies the quote's client, organization,
     * location, description and every line including its price.
     */
    private Job createFromQuote(JobCreateRequest request) {
        rejectWalkInFields(request);

        Quote quote = quoteRepo.findById(request.quoteId())
                .orElseThrow(() -> new QuoteNotFoundException(request.quoteId()));

        // uq_job_quote is a partial unique index, so the database would reject this too
        // -- but only as an opaque constraint violation. Checked here so the caller is
        // told which job already exists.
        jobRepo.findByQuoteId(quote.getId()).ifPresent(existing -> {
            throw new ConflictException("Quote " + quote.getId() + " already has job "
                    + existing.getId() + ". A quote can become at most one job.");
        });

        Job job = new Job();
        job.setQuote(quote);
        job.setClient(quote.getClient());
        job.setOrganization(quote.getOrganization());
        job.setLocation(quote.getLocation());
        job.setDescription(request.description() != null ? request.description() : quote.getDescription());
        job.setStatus(request.status() != null ? request.status() : JobStatus.SCHEDULED);
        job.setScheduledAt(request.scheduledAt());

        for (QuoteLineItem quoted : quote.getItems()) {
            JobLineItem item = new JobLineItem();
            item.setService(quoted.getService());
            item.setUnitPrice(quoted.getUnitPrice());
            item.setQuantity(quoted.getQuantity());
            item.setDescription(quoted.getDescription());
            item.setStatus(JobServiceStatus.PENDING);
            job.addItem(item);
        }

        return jobRepo.save(job);
    }

    /** Work that never had a quote. Resolves client and location the same way the funnel does. */
    private Job createWalkIn(JobCreateRequest request) {
        if (request.organizationSlug() == null || request.organizationSlug().isBlank()
                || request.client() == null || request.location() == null
                || request.services() == null || request.services().isEmpty()) {
            throw new BadRequestException(
                    "Without a quoteId, a job needs organizationSlug, client, location and "
                            + "at least one service.");
        }

        Organization organization = organizationLookup.requireBySlug(request.organizationSlug());
        Client client = clientResolver.upsert(request.client());
        Location location = locationResolver.resolve(client, request.location());
        Map<Long, ServiceOffering> services = offeredServiceResolver.requireAllOffered(
                organization,
                request.services().stream().map(JobLineItemCreateRequest::serviceId).toList());

        Job job = new Job();
        job.setClient(client);
        job.setOrganization(organization);
        job.setLocation(location);
        job.setDescription(request.description());
        job.setStatus(request.status() != null ? request.status() : JobStatus.SCHEDULED);
        job.setScheduledAt(request.scheduledAt());

        // No dedupe here, unlike quotes: job_service has no uniqueness constraint,
        // because the same service twice on one job is legitimate work.
        for (JobLineItemCreateRequest requested : request.services()) {
            JobLineItem item = new JobLineItem();
            item.setService(services.get(requested.serviceId()));
            item.setUnitPrice(requested.unitPrice());
            item.setQuantity(requested.quantity());
            item.setDescription(requested.description());
            item.setStatus(requested.status() != null ? requested.status() : JobServiceStatus.PENDING);
            job.addItem(item);
        }

        return jobRepo.save(job);
    }

    /**
     * Rejected rather than ignored: silently dropping a client the caller supplied
     * alongside a quoteId would look like it had been applied.
     */
    private static void rejectWalkInFields(JobCreateRequest request) {
        if (request.organizationSlug() != null || request.client() != null
                || request.location() != null || request.services() != null) {
            throw new BadRequestException(
                    "When quoteId is given, organizationSlug, client, location and services "
                            + "are taken from the quote and must be omitted.");
        }
    }

    Job require(Long id) {
        return jobRepo.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }

    private Map<Long, LineTotals> loadTotals(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = jobs.stream().map(Job::getId).toList();
        return lineItemRepo.findTotalsByJobIds(ids).stream()
                .collect(Collectors.toMap(LineTotals::getOwnerId, Function.identity()));
    }

    private static JobSummary toSummary(Job job, LineTotals totals) {
        // A job with no lines has no row in the grouped result at all.
        int itemCount = totals == null ? 0 : (int) totals.getItemCount();

        return new JobSummary(
                job.getId(),
                job.getQuote() == null ? null : job.getQuote().getId(),
                job.getOrganization().getSlug(),
                job.getStatus(),
                job.getScheduledAt(),
                job.getCreatedAt(),
                job.getClient().fullName(),
                job.getClient().getEmail(),
                job.getLocation().getCity(),
                itemCount,
                totals == null ? null : totals.getTotal());
    }
}
