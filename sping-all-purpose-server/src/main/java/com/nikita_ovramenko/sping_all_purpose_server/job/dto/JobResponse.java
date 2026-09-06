package com.nikita_ovramenko.sping_all_purpose_server.job.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;

/**
 * A job in full.
 *
 * <p>client, organization and location are read from the job itself rather than through
 * the quote, deliberately: a job can be relocated or reassigned after the quote was
 * written, and the quote must not change when it is.
 *
 * @param quoteId the originating quote, or null for a walk-in
 * @param total   sum of the priced lines, or null when nothing is priced yet
 */
public record JobResponse(
        Long id,
        Long quoteId,
        String organizationSlug,
        JobStatus status,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        ClientSummary client,
        LocationSummary location,
        List<JobLineItemResponse> services,
        String description,
        BigDecimal total) {
}
