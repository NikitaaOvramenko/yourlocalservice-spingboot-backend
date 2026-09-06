package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;

/**
 * One row in an admin quote list.
 *
 * <p>Deliberately not the full QuoteResponse. Building that touches items, each item's
 * service, and the picture collection -- for a page of twenty quotes that is a great
 * many lazy loads, and fetching Quote's two collections in one entity graph is exactly
 * the case Hibernate rejects with MultipleBagFetchException. The detail endpoint returns
 * the full record; a list does not need it.
 *
 * @param total sum of the line items, or null when no line has been priced yet
 */
public record QuoteSummary(
        Long id,
        String organizationSlug,
        QuoteStatus status,
        Instant createdAt,
        Instant expiresAt,
        String clientName,
        String clientEmail,
        String city,
        int itemCount,
        BigDecimal total) {
}
