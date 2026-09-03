package com.nikita_ovramenko.sping_all_purpose_server.organization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;

@Repository
public interface OrganizationRepo extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlugIgnoreCase(String slug);

    Optional<Organization> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);
}
