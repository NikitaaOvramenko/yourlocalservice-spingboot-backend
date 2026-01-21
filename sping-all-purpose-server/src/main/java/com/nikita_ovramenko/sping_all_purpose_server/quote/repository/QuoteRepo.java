package com.nikita_ovramenko.sping_all_purpose_server.quote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

@Repository
public interface QuoteRepo extends JpaRepository<Quote, Long> {

}
