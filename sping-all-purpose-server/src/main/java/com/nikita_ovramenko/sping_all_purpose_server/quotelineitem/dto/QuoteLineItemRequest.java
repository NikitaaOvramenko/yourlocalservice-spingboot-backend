package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuoteLineItemRequest(
        @NotNull Long serviceId,
        @NotNull @Min(1) @Max(999) Integer quantity,
        @Size(max = 1000) String description) {
}
