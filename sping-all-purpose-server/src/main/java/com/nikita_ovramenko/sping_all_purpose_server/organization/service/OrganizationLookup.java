package com.nikita_ovramenko.sping_all_purpose_server.organization.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.organization.exception.OrganizationNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.repository.OrganizationRepo;

/** Resolves the {slug} path variable to an organization, or 404s. */
@Service
public class OrganizationLookup {

    private final OrganizationRepo organizationRepo;

    public OrganizationLookup(OrganizationRepo organizationRepo) {
        this.organizationRepo = organizationRepo;
    }

    @Transactional(readOnly = true)
    public Organization requireBySlug(String slug) {
        return organizationRepo.findBySlugIgnoreCase(slug)
                .filter(Organization::isActive)
                .orElseThrow(() -> new OrganizationNotFoundException(slug));
    }
}
