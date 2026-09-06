package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto;

import java.math.BigDecimal;

import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Partial update; a null field is left unchanged. The service is not re-assignable. */
public record JobLineItemUpdateRequest(
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @Min(1) @Max(999) Integer quantity,
        @Size(max = 1000) String description,
        JobServiceStatus status) {
}
