package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import java.time.Instant;

import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;

import jakarta.validation.constraints.Size;

/**
 * Partial update. A null field means "leave unchanged", which is applied uniformly
 * across the admin API.
 *
 * <p>The consequence is that expiresAt cannot be cleared through this endpoint, only
 * moved. Clearing it needs either a dedicated flag or JsonNullable; neither is worth a
 * dependency yet.
 */
public record QuoteUpdateRequest(
        QuoteStatus status,
        @Size(max = 4000) String description,
        Instant expiresAt) {
}
