package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto;

import java.math.BigDecimal;

import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;

/**
 * One line of work on a job.
 *
 * <p>Carries its own status, which a quote line does not: job.status is the human-set
 * state of the whole job, while this tracks progress of one service within it.
 */
public record JobLineItemResponse(
        Long id,
        Long serviceId,
        String serviceName,
        String serviceSlug,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        String description,
        JobServiceStatus status) {
}
