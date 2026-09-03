package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A service in the global catalog, e.g. "Fridge Repair".
 *
 * <p>Named ServiceOffering rather than Service to avoid colliding with
 * org.springframework.stereotype.Service; the table is still "service".
 */
@Entity
@Table(name = "service")
@Getter
@Setter
@NoArgsConstructor
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    /** Stable identifier for the API, and the fallback the legacy adapter matches on. */
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
