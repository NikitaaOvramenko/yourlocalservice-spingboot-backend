package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import java.time.Instant;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;

public record QuoteResponse(
        Long id,
        String organizationSlug,
        QuoteStatus status,
        Instant createdAt,
        Instant expiresAt,
        ClientSummary client,
        LocationSummary location,
        List<QuoteLineItemResponse> services,
        String description,
        List<String> pictureKeys) {
}
