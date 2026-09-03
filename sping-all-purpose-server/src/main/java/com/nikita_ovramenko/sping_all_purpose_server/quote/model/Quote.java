package com.nikita_ovramenko.sping_all_purpose_server.quote.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.common.model.Auditable;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;

/**
 * A client's request for work from one organization.
 *
 * <p>The free-text workType and the list-of-strings serviceType are gone: the former
 * is now the organization association, the latter QuoteLineItem rows against the
 * service catalog.
 *
 * <p>There is deliberately no inverse @OneToOne to Job. An inverse one-to-one cannot
 * be made lazy without bytecode enhancement, so mapping it would make Hibernate
 * select the job on every quote load. Use JobRepo.findByQuoteId instead.
 */
@Entity
@Table(name = "quote", indexes = {
        @Index(name = "ix_quote_client", columnList = "client_id"),
        @Index(name = "ix_quote_organization", columnList = "organization_id"),
        @Index(name = "ix_quote_location", columnList = "location_id"),
        @Index(name = "ix_quote_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Quote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuoteStatus status;

    /** Set when the org prices and sends the quote; null on a fresh submission. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "quote", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteLineItem> items = new ArrayList<>();

    /** S3 object keys uploaded via the presigned PUT flow, in the order submitted. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "quote_picture", joinColumns = @JoinColumn(name = "quote_id"))
    @Column(name = "object_key", nullable = false, length = 1024)
    @OrderColumn(name = "position")
    private List<String> pictures = new ArrayList<>();

    public void addItem(QuoteLineItem item) {
        item.setQuote(this);
        this.items.add(item);
    }

    /** True once a sent quote is past its expiry, without needing a status sweeper. */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
