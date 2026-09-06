package com.nikita_ovramenko.sping_all_purpose_server.job.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;

/**
 * One row in an admin job list. Same reasoning as QuoteSummary: returning the full
 * response for every row would mean a lazy load per row for the line items.
 *
 * @param quoteId the quote this came from, or null for a walk-in
 */
public record JobSummary(
        Long id,
        Long quoteId,
        String organizationSlug,
        JobStatus status,
        Instant scheduledAt,
        Instant createdAt,
        String clientName,
        String clientEmail,
        String city,
        int itemCount,
        BigDecimal total) {
}
