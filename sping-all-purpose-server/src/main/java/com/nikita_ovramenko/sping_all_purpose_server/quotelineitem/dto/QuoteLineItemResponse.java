package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto;

import java.math.BigDecimal;

public record QuoteLineItemResponse(
        Long id, Long serviceId, String serviceName, String serviceSlug,
        BigDecimal unitPrice, Integer quantity, BigDecimal lineTotal, String description) {
}
