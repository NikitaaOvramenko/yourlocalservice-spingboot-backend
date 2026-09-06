package com.nikita_ovramenko.sping_all_purpose_server.job.service;

import org.springframework.data.jpa.domain.Specification;

import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;

import jakarta.persistence.criteria.JoinType;

/** Filters for the admin job list. Each returns null when its filter is absent. */
public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> organizationSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("organization").get("slug")), slug.toLowerCase());
    }

    public static Specification<Job> status(JobStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Job> clientEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("client").get("email")), email.toLowerCase());
    }

    /**
     * Loads client, location and organization alongside the job so a page of summaries
     * is one query rather than one per row.
     *
     * <p>The result-type guard is load-bearing: Page runs a count query against the same
     * specification, and a count query cannot carry a fetch join -- without the check,
     * paging fails outright.
     */
    public static Specification<Job> fetchSummaryAssociations() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("client", JoinType.LEFT);
                root.fetch("location", JoinType.LEFT);
                root.fetch("organization", JoinType.LEFT);
            }
            return null;
        };
    }
}
