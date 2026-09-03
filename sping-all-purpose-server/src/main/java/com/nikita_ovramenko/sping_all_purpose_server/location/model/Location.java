package com.nikita_ovramenko.sping_all_purpose_server.location.model;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A client's address.
 *
 * <p>The unique constraint is what stops a fresh row being created on every quote
 * submission. Every column in it must be NOT NULL: Postgres treats NULLs as distinct
 * in a unique index, so a single nullable column would silently defeat it. Callers go
 * through LocationResolver, which normalizes before comparing.
 */
@Entity
@Table(name = "location",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_location_client_address",
                columnNames = { "client_id", "country", "province_state", "city", "street", "postal_code" }),
        indexes = @Index(name = "ix_location_client", columnList = "client_id"))
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Country country;

    /** US state or Canadian province. Empty string rather than null, never validated. */
    @Column(name = "province_state", nullable = false, length = 100)
    private String provinceState = "";

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
}
