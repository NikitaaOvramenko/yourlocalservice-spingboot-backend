package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;

@Repository
public interface JobLineItemRepo extends JpaRepository<JobLineItem, Long> {

    List<JobLineItem> findByJobId(Long jobId);
}
