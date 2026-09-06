package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Adds a service to an existing quote.
 *
 * <p>unitPrice is optional: staff often add the line first and price it later. Digits
 * mirrors the NUMERIC(12,2) column, so an over-precise price is a 400 rather than a
 * silent rounding.
 */
public record QuoteLineItemCreateRequest(
        @NotNull Long serviceId,
        @NotNull @Min(1) @Max(999) Integer quantity,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @Size(max = 1000) String description) {
}
