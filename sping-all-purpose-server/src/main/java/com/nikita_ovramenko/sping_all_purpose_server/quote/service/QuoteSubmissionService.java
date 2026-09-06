package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.service.ClientResolver;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.service.LocationResolver;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quote.event.QuoteSubmittedEvent;
import com.nikita_ovramenko.sping_all_purpose_server.quote.mapper.QuoteMapper;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * The single write path for quote submissions. Both the new
 * POST /api/orgs/{slug}/quotes and the legacy POST /api/email/form land here.
 */
@Service
public class QuoteSubmissionService {

    private final QuoteRepo quoteRepo;
    private final ClientResolver clientResolver;
    private final OfferedServiceResolver offeredServiceResolver;
    private final OrganizationLookup organizationLookup;
    private final LocationResolver locationResolver;
    private final QuoteMapper quoteMapper;
    private final ApplicationEventPublisher eventPublisher;

    public QuoteSubmissionService(QuoteRepo quoteRepo, ClientResolver clientResolver,
            OfferedServiceResolver offeredServiceResolver,
            OrganizationLookup organizationLookup, LocationResolver locationResolver,
            QuoteMapper quoteMapper, ApplicationEventPublisher eventPublisher) {
        this.quoteRepo = quoteRepo;
        this.clientResolver = clientResolver;
        this.offeredServiceResolver = offeredServiceResolver;
        this.organizationLookup = organizationLookup;
        this.locationResolver = locationResolver;
        this.quoteMapper = quoteMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * A member of the public submitting the web form. Persists the quote and triggers
     * the confirmation and lead-notification emails.
     */
    @Transactional
    public QuoteResponse submit(String orgSlug, QuoteRequest request) {
        Organization organization = organizationLookup.requireBySlug(orgSlug);
        Quote saved = createQuote(organization, request);
        QuoteResponse response = quoteMapper.toResponse(saved);

        // Emails are sent by a listener after this transaction commits, so an SMTP
        // failure can no longer discard a quote the client believes was submitted. The
        // event carries the finished response because it is built here, while the
        // session is open -- the listener would otherwise be holding a detached entity.
        eventPublisher.publishEvent(new QuoteSubmittedEvent(response, organization));

        return response;
    }

    /**
     * Staff entering a lead that arrived by phone or in person.
     *
     * <p>Identical to {@link #submit} except that it publishes no event, and so sends no
     * mail. That difference is the entire point: a client who never filled in the web
     * form must not receive "thanks for your request", and the business does not need a
     * new-lead notification about a lead it just typed in itself.
     */
    @Transactional
    public QuoteResponse createForStaff(String orgSlug, QuoteRequest request) {
        Organization organization = organizationLookup.requireBySlug(orgSlug);
        return quoteMapper.toResponse(createQuote(organization, request));
    }

    /**
     * Everything both entry points share: client upsert, location dedup, service
     * validation, and persistence. Kept private so the only difference between a public
     * submission and a staff entry stays visible in the two methods above.
     */
    private Quote createQuote(Organization organization, QuoteRequest request) {
        Client client = clientResolver.upsert(request.client());
        Location location = locationResolver.resolve(client, request.location());
        Map<Long, ServiceOffering> services = offeredServiceResolver.requireAllOffered(
                organization, request.services().stream().map(QuoteLineItemRequest::serviceId).toList());

        Quote quote = new Quote();
        quote.setOrganization(organization);
        quote.setClient(client);
        quote.setLocation(location);
        quote.setDescription(request.description());
        quote.setStatus(QuoteStatus.SUBMITTED);

        for (QuoteLineItemRequest requested : dedupeByService(request.services())) {
            QuoteLineItem item = new QuoteLineItem();
            item.setService(services.get(requested.serviceId()));
            item.setQuantity(requested.quantity());
            item.setDescription(requested.description());
            quote.addItem(item);
        }

        if (request.pictureKeys() != null) {
            quote.getPictures().addAll(request.pictureKeys());
        }

        return quoteRepo.save(quote);
    }

    /** uq_quote_service forbids the same service twice on one quote; keep the first mention. */
    private static List<QuoteLineItemRequest> dedupeByService(List<QuoteLineItemRequest> requested) {
        Set<Long> seen = new LinkedHashSet<>();
        List<QuoteLineItemRequest> unique = new ArrayList<>();
        for (QuoteLineItemRequest item : requested) {
            if (seen.add(item.serviceId())) {
                unique.add(item);
            }
        }
        return unique;
    }
}
