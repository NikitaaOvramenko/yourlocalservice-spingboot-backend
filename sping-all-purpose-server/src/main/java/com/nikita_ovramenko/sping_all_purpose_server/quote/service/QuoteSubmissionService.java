package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.service.LocationResolver;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository.OrganizationServiceOfferingRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quote.event.QuoteSubmittedEvent;
import com.nikita_ovramenko.sping_all_purpose_server.quote.mapper.QuoteMapper;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository.ServiceOfferingRepo;

/**
 * The single write path for quote submissions. Both the new
 * POST /api/orgs/{slug}/quotes and the legacy POST /api/email/form land here.
 */
@Service
public class QuoteSubmissionService {

    private final QuoteRepo quoteRepo;
    private final ClientRepo clientRepo;
    private final ServiceOfferingRepo serviceOfferingRepo;
    private final OrganizationServiceOfferingRepo organizationServiceOfferingRepo;
    private final OrganizationLookup organizationLookup;
    private final LocationResolver locationResolver;
    private final QuoteMapper quoteMapper;
    private final ApplicationEventPublisher eventPublisher;

    public QuoteSubmissionService(QuoteRepo quoteRepo, ClientRepo clientRepo,
            ServiceOfferingRepo serviceOfferingRepo,
            OrganizationServiceOfferingRepo organizationServiceOfferingRepo,
            OrganizationLookup organizationLookup, LocationResolver locationResolver,
            QuoteMapper quoteMapper, ApplicationEventPublisher eventPublisher) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
        this.serviceOfferingRepo = serviceOfferingRepo;
        this.organizationServiceOfferingRepo = organizationServiceOfferingRepo;
        this.organizationLookup = organizationLookup;
        this.locationResolver = locationResolver;
        this.quoteMapper = quoteMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public QuoteResponse submit(String orgSlug, QuoteRequest request) {
        Organization organization = organizationLookup.requireBySlug(orgSlug);
        Client client = upsertClient(request.client());
        Location location = locationResolver.resolve(client, request.location());
        Map<Long, ServiceOffering> services = resolveOfferedServices(organization, request.services());

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

        Quote saved = quoteRepo.save(quote);

        // Emails are sent by a listener after this transaction commits, so an SMTP
        // failure can no longer discard a quote the client believes was submitted.
        eventPublisher.publishEvent(new QuoteSubmittedEvent(saved.getId()));

        return quoteMapper.toResponse(saved);
    }

    private Client upsertClient(ClientRequest request) {
        Client client = clientRepo.findByEmailIgnoreCase(request.email()).orElseGet(Client::new);
        // NOTE: this overwrites an existing client's details on every submission, which
        // is the pre-existing behaviour. With no authentication it means anyone who
        // knows an email address can rewrite that person's record. Left as-is here;
        // it belongs with the auth work rather than this refactor.
        client.setEmail(request.email());
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setPhone(request.phone());
        return clientRepo.save(client);
    }

    /**
     * Loads the requested services and checks the organization actually offers them.
     * Two distinct failures: unknown to the catalog entirely (400) versus real but not
     * offered by this organization (422).
     */
    private Map<Long, ServiceOffering> resolveOfferedServices(
            Organization organization, List<QuoteLineItemRequest> requested) {

        Set<Long> requestedIds = requested.stream()
                .map(QuoteLineItemRequest::serviceId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ServiceOffering> found = serviceOfferingRepo.findAllById(requestedIds);
        // findAllById silently drops ids it cannot find, so the size check is required.
        if (found.size() != requestedIds.size()) {
            Set<Long> foundIds = found.stream().map(ServiceOffering::getId).collect(Collectors.toSet());
            Set<Long> unknown = new LinkedHashSet<>(requestedIds);
            unknown.removeAll(foundIds);
            throw new UnknownServiceException(unknown);
        }

        Set<Long> offeredIds = organizationServiceOfferingRepo
                .findServiceIdsByOrganizationId(organization.getId());
        Set<Long> notOffered = new LinkedHashSet<>(requestedIds);
        notOffered.removeAll(offeredIds);
        if (!notOffered.isEmpty()) {
            throw new ServiceNotOfferedException(organization.getSlug(), notOffered);
        }

        return found.stream().collect(Collectors.toMap(ServiceOffering::getId, Function.identity()));
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
