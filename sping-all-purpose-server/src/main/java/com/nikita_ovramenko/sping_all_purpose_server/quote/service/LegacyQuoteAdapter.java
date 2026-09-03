package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.exception.InvalidCountryException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.exception.OrganizationNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.repository.OrganizationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository.ServiceOfferingRepo;

/**
 * Translates the legacy POST /api/email/form payload onto the new model so the
 * currently deployed frontends keep working unchanged.
 *
 * <p>The old payload carries a free-text workType (now an Organization) and a list of
 * free-text service names (now ServiceOffering rows). Both are resolved against the
 * catalog; nothing is auto-created from free text, or every frontend typo would become
 * a permanent service row.
 */
@Service
public class LegacyQuoteAdapter {

    private final OrganizationRepo organizationRepo;
    private final ServiceOfferingRepo serviceOfferingRepo;
    private final QuoteSubmissionService quoteSubmissionService;

    public LegacyQuoteAdapter(OrganizationRepo organizationRepo, ServiceOfferingRepo serviceOfferingRepo,
            QuoteSubmissionService quoteSubmissionService) {
        this.organizationRepo = organizationRepo;
        this.serviceOfferingRepo = serviceOfferingRepo;
        this.quoteSubmissionService = quoteSubmissionService;
    }

    public void submit(QuoteDto dto) {
        Organization organization = resolveOrganization(dto.workType());
        List<QuoteLineItemRequest> services = resolveServices(dto.service());

        QuoteRequest request = new QuoteRequest(
                new ClientRequest(dto.name(), dto.lastname(), dto.email(), dto.phone()),
                new LocationRequest(
                        parseCountry(dto.country()),
                        // The legacy form has no province/state field.
                        "",
                        dto.town(),
                        dto.street(),
                        dto.postal_code()),
                services,
                dto.description(),
                dto.images() == null ? List.of() : dto.images());

        quoteSubmissionService.submit(organization.getSlug(), request);
    }

    /**
     * Resolves the free-text workType to an organization by slug, then by name.
     *
     * <p>There is no byte-exact mapping table any more, so this only succeeds when the
     * posted workType matches an organization's slug or display name. The strings the
     * original sites posted ("Junk Removal", "Construction") no longer resolve -- this
     * endpoint is only for frontends that already send the organization's own name.
     * Previously an unrecognised work type silently produced a null From address; it
     * now fails loudly with a 404.
     */
    private Organization resolveOrganization(String workType) {
        if (workType == null || workType.isBlank()) {
            throw new OrganizationNotFoundException(String.valueOf(workType));
        }
        String trimmed = workType.trim();
        return organizationRepo.findBySlugIgnoreCase(slugify(trimmed))
                .or(() -> organizationRepo.findByNameIgnoreCase(trimmed))
                .filter(Organization::isActive)
                .orElseThrow(() -> new OrganizationNotFoundException(trimmed));
    }

    private List<QuoteLineItemRequest> resolveServices(List<String> serviceNames) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            throw new UnknownServiceException(List.of("<none supplied>"));
        }

        List<QuoteLineItemRequest> resolved = new ArrayList<>();
        Set<String> unresolved = new LinkedHashSet<>();

        for (String name : serviceNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            ServiceOffering match = serviceOfferingRepo.findByNameIgnoreCase(trimmed)
                    .or(() -> serviceOfferingRepo.findBySlugIgnoreCase(slugify(trimmed)))
                    .orElse(null);
            if (match == null) {
                unresolved.add(trimmed);
            } else {
                resolved.add(new QuoteLineItemRequest(match.getId(), 1, null));
            }
        }

        if (!unresolved.isEmpty()) {
            throw new UnknownServiceException(unresolved);
        }
        if (resolved.isEmpty()) {
            throw new UnknownServiceException(List.of("<none supplied>"));
        }
        return resolved;
    }

    /** Country.valueOf used to throw straight out of the service, turning bad input into a 500. */
    private static Country parseCountry(String country) {
        if (country == null) {
            throw new InvalidCountryException(null);
        }
        try {
            return Country.valueOf(country.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCountryException(country);
        }
    }

    static String slugify(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
