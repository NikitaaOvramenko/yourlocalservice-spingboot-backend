package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.job.exception.JobNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.job.mapper.JobMapper;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;
import com.nikita_ovramenko.sping_all_purpose_server.job.repository.JobRepo;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.exception.JobLineItemNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository.JobLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * The individual pieces of work on a job.
 *
 * <p>As with quote lines, every method takes the job id as well as the item id and
 * refuses an item belonging to a different job, so the nested URL is a real constraint
 * rather than decoration.
 */
@Service
public class JobLineItemService {

    private final JobRepo jobRepo;
    private final JobLineItemRepo lineItemRepo;
    private final OfferedServiceResolver offeredServiceResolver;

    public JobLineItemService(JobRepo jobRepo, JobLineItemRepo lineItemRepo,
            OfferedServiceResolver offeredServiceResolver) {
        this.jobRepo = jobRepo;
        this.lineItemRepo = lineItemRepo;
        this.offeredServiceResolver = offeredServiceResolver;
    }

    @Transactional(readOnly = true)
    public List<JobLineItemResponse> list(Long jobId) {
        requireJob(jobId);
        return lineItemRepo.findByJobIdOrderByIdAsc(jobId).stream()
                .map(JobMapper::toServiceResponse)
                .toList();
    }

    @Transactional
    public JobLineItemResponse add(Long jobId, JobLineItemCreateRequest request) {
        Job job = requireJob(jobId);
        ServiceOffering service = offeredServiceResolver.requireOffered(
                job.getOrganization(), request.serviceId());

        // No duplicate check: job_service has no unique constraint on (job, service),
        // because doing the same service twice on one job is legitimate.
        JobLineItem item = new JobLineItem();
        item.setJob(job);
        item.setService(service);
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDescription(request.description());
        item.setStatus(request.status() != null ? request.status() : JobServiceStatus.PENDING);

        return JobMapper.toServiceResponse(lineItemRepo.save(item));
    }

    /** Partial update: a null field is left unchanged. The service is not re-assignable. */
    @Transactional
    public JobLineItemResponse update(Long jobId, Long itemId, JobLineItemUpdateRequest request) {
        JobLineItem item = requireItemOnJob(jobId, itemId);

        if (request.unitPrice() != null) {
            item.setUnitPrice(request.unitPrice());
        }
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.status() != null) {
            item.setStatus(request.status());
        }

        return JobMapper.toServiceResponse(lineItemRepo.save(item));
    }

    @Transactional
    public void delete(Long jobId, Long itemId) {
        lineItemRepo.delete(requireItemOnJob(jobId, itemId));
    }

    private Job requireJob(Long jobId) {
        return jobRepo.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    /**
     * An item addressed under the wrong job is reported as not found, not as a mismatch:
     * the caller has no business learning that the id exists elsewhere.
     */
    private JobLineItem requireItemOnJob(Long jobId, Long itemId) {
        JobLineItem item = lineItemRepo.findById(itemId)
                .orElseThrow(() -> new JobLineItemNotFoundException(jobId, itemId));
        if (!item.getJob().getId().equals(jobId)) {
            throw new JobLineItemNotFoundException(jobId, itemId);
        }
        return item;
    }
}
