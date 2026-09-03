package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;

@Repository
public interface QuoteLineItemRepo extends JpaRepository<QuoteLineItem, Long> {

    List<QuoteLineItem> findByQuoteId(Long quoteId);
}
