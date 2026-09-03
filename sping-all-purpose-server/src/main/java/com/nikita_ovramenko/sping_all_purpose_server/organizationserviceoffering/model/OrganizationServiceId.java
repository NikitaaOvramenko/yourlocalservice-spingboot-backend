package com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite key for {@link OrganizationServiceOffering}.
 *
 * <p>equals/hashCode and Serializable are mandatory here, not stylistic: without them
 * Hibernate's identity map misbehaves and existsById returns garbage. They are written
 * out by hand rather than via Lombok because this is a value type keyed on two Longs,
 * which is exactly the case where @EqualsAndHashCode on entities is unsafe.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class OrganizationServiceId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "service_id")
    private Long serviceId;

    public OrganizationServiceId(Long organizationId, Long serviceId) {
        this.organizationId = organizationId;
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrganizationServiceId other)) {
            return false;
        }
        return Objects.equals(organizationId, other.organizationId)
                && Objects.equals(serviceId, other.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, serviceId);
    }
}
