package com.nikita_ovramenko.sping_all_purpose_server.organization.controller;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.organization.dto.OrganizationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository.OrganizationServiceOfferingRepo;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceSummary;

/**
 * Public, per-organization catalog.
 *
 * <p>The services endpoint is required rather than a nicety: without it a frontend
 * that knows only its slug has no way to discover valid serviceId values, and the
 * quote POST is unusable. Results are scoped through organization_service so one
 * org's page never sees another org's catalog.
 */
@RestController
@RequestMapping("/api/orgs/{slug}")
public class OrganizationController {

    private final OrganizationLookup organizationLookup;
    private final OrganizationServiceOfferingRepo organizationServiceOfferingRepo;

    public OrganizationController(OrganizationLookup organizationLookup,
            OrganizationServiceOfferingRepo organizationServiceOfferingRepo) {
        this.organizationLookup = organizationLookup;
        this.organizationServiceOfferingRepo = organizationServiceOfferingRepo;
    }

    @GetMapping
    public OrganizationSummary get(@PathVariable String slug) {
        Organization organization = organizationLookup.requireBySlug(slug);
        return new OrganizationSummary(organization.getId(), organization.getName(), organization.getSlug());
    }

    @GetMapping("/services")
    @Transactional(readOnly = true)
    public List<ServiceSummary> services(@PathVariable String slug) {
        // Resolve first so an unknown slug is a 404 rather than an empty list.
        organizationLookup.requireBySlug(slug);
        return organizationServiceOfferingRepo.findActiveServicesByOrganizationSlug(slug).stream()
                .map(s -> new ServiceSummary(s.getId(), s.getName(), s.getSlug(), s.getDescription()))
                .toList();
    }
}
