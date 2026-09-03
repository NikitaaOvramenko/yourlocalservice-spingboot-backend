package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model;

import java.math.BigDecimal;

import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

/**
 * One requested service on a quote.
 *
 * <p>Holds no client / organization / location references -- those belong to the
 * owning Quote. Replaces the old element collection of free-text service strings.
 */
@Entity
@Table(name = "quote_service",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quote_service", columnNames = { "quote_id", "service_id" }),
        indexes = {
                @Index(name = "ix_quote_service_quote", columnList = "quote_id"),
                @Index(name = "ix_quote_service_service", columnList = "service_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class QuoteLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    /**
     * Price per unit, not the line total. Null on a client submission -- the client
     * asks for work, the organization prices it afterwards.
     */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(columnDefinition = "text")
    private String description;

    /** Convenience only; the total is computed, never stored. */
    public BigDecimal lineTotal() {
        if (unitPrice == null || quantity == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
