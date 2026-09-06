package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.common.dto.LineTotals;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;

@Repository
public interface JobLineItemRepo extends JpaRepository<JobLineItem, Long> {

    List<JobLineItem> findByJobIdOrderByIdAsc(Long jobId);

    /** Line counts and totals for a page of jobs, in one query. See {@link LineTotals}. */
    @Query("""
            select i.job.id as ownerId,
                   count(i) as itemCount,
                   sum(i.unitPrice * i.quantity) as total
            from JobLineItem i
            where i.job.id in :jobIds
            group by i.job.id
            """)
    List<LineTotals> findTotalsByJobIds(@Param("jobIds") Collection<Long> jobIds);
}
