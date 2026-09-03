package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums;

/** Per-line progress within a job. See JobStatus for how the two relate. */
public enum JobServiceStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}
