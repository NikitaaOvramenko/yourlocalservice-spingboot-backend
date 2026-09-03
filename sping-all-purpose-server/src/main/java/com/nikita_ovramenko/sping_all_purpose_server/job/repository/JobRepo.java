package com.nikita_ovramenko.sping_all_purpose_server.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;

@Repository
public interface JobRepo extends JpaRepository<Job, Long> {

    /**
     * The inverse of Job.quote. Queried rather than mapped as @OneToOne(mappedBy) on
     * Quote: an inverse @OneToOne cannot be lazy without bytecode enhancement, so
     * mapping it would make Hibernate select the job on every single quote load.
     */
    Optional<Job> findByQuoteId(Long quoteId);

    List<Job> findByClientId(Long clientId);

    List<Job> findByOrganizationId(Long organizationId);
}
