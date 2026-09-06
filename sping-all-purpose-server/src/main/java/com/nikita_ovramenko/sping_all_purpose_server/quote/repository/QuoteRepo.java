package com.nikita_ovramenko.sping_all_purpose_server.quote.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

@Repository
// JpaSpecificationExecutor rather than a @Query with (:status is null or ...):
// an untyped null bind parameter fails on Postgres with "could not determine data
// type of parameter". A Specification emits no SQL at all for an absent filter.
public interface QuoteRepo extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    List<Quote> findByClientId(Long clientId);
}
