package com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.model;

import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * Which services an organization offers.
 *
 * <p>An explicit join entity rather than @ManyToMany. The hot query is "does org Y
 * offer service X?", which here is a single primary-key probe; with @ManyToMany it
 * would mean loading the org's entire service list to check one. Hibernate also
 * delete-all-then-reinserts a dirtied @ManyToMany collection, and a join entity
 * leaves room for price_override / display_order later.
 */
@Entity
@Table(name = "organization_service")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationServiceOffering {

    @EmbeddedId
    private OrganizationServiceId id;

    @MapsId("organizationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @MapsId("serviceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    public OrganizationServiceOffering(Organization organization, ServiceOffering service) {
        this.organization = organization;
        this.service = service;
        this.id = new OrganizationServiceId(organization.getId(), service.getId());
    }
}
