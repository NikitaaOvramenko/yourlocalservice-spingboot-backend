package com.nikita_ovramenko.sping_all_purpose_server.job.dto;

import java.time.Instant;

import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;

import jakarta.validation.constraints.Size;

/**
 * Partial update; a null field is left unchanged.
 *
 * <p>Transitions are not constrained to a state machine: any JobStatus is accepted,
 * including moving backwards, because real schedules do that.
 */
public record JobUpdateRequest(
        JobStatus status,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        @Size(max = 4000) String description) {
}
