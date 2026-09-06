package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto;

import java.math.BigDecimal;

import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Adds a service to a job.
 *
 * <p>Unlike a quote line there is no uniqueness constraint on (job, service): the same
 * service appearing twice on one job is legitimate work, so this never conflicts.
 *
 * <p>status defaults to PENDING when omitted.
 */
public record JobLineItemCreateRequest(
        @NotNull Long serviceId,
        @NotNull @Min(1) @Max(999) Integer quantity,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
        @Size(max = 1000) String description,
        JobServiceStatus status) {
}
