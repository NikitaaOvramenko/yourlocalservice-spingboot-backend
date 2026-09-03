package com.nikita_ovramenko.sping_all_purpose_server.job.enums;

/**
 * Authoritative, human-set status of the job as a whole.
 *
 * <p>JobLineItem carries its own per-line status; this one wins. Nothing in the
 * database can enforce that relationship, so the service layer owns the rule
 * (a job should not reach COMPLETED while a line is still IN_PROGRESS).
 */
public enum JobStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
