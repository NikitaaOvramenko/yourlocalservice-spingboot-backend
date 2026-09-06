package com.nikita_ovramenko.sping_all_purpose_server.job.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;

@Repository
// JpaSpecificationExecutor rather than a @Query with (:status is null or ...): an
// untyped null bind parameter fails on Postgres with "could not determine data type of
// parameter". A Specification emits no SQL at all for an absent filter.
public interface JobRepo extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    /**
     * Backs the check that a quote does not already have a job. uq_job_quote is a
     * partial unique index (WHERE quote_id IS NOT NULL), so the database would catch a
     * duplicate anyway -- but only as an opaque constraint violation.
     */
    Optional<Job> findByQuoteId(Long quoteId);
}
