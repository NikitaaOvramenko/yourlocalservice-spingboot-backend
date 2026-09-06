package com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.model.OrganizationServiceId;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.model.OrganizationServiceOffering;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

@Repository
public interface OrganizationServiceOfferingRepo
        extends JpaRepository<OrganizationServiceOffering, OrganizationServiceId> {

    /**
     * The service ids an organization offers, as one query -- used to validate a whole
     * quote request against the catalog without an N+1.
     */
    @Query("select oso.id.serviceId from OrganizationServiceOffering oso "
            + "where oso.id.organizationId = :orgId")
    Set<Long> findServiceIdsByOrganizationId(@Param("orgId") Long orgId);

    /**
     * Every offering row for one organization, active or not.
     *
     * <p>Distinct from the public listing below, which filters on active: this backs the
     * admin screen, where seeing a deactivated service is the point.
     */
    List<OrganizationServiceOffering> findAllByOrganizationId(Long organizationId);

    /**
     * Public catalog for one org's site. Scoped through the join table on purpose: a
     * plain findAll() on ServiceOffering would leak every org's catalog to every page.
     */
    @Query("select oso.service from OrganizationServiceOffering oso "
            + "where lower(oso.organization.slug) = lower(:slug) "
            + "and oso.organization.active = true and oso.service.active = true "
            + "order by oso.service.name")
    List<ServiceOffering> findActiveServicesByOrganizationSlug(@Param("slug") String slug);
}
