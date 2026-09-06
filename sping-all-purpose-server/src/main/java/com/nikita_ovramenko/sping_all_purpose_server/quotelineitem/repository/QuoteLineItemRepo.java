package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.common.dto.LineTotals;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;

@Repository
public interface QuoteLineItemRepo extends JpaRepository<QuoteLineItem, Long> {

    List<QuoteLineItem> findByQuoteIdOrderByIdAsc(Long quoteId);

    boolean existsByQuoteIdAndServiceId(Long quoteId, Long serviceId);

    /** Line counts and totals for a page of quotes, in one query. See {@link LineTotals}. */
    @Query("""
            select i.quote.id as ownerId,
                   count(i) as itemCount,
                   sum(i.unitPrice * i.quantity) as total
            from QuoteLineItem i
            where i.quote.id in :quoteIds
            group by i.quote.id
            """)
    List<LineTotals> findTotalsByQuoteIds(@Param("quoteIds") Collection<Long> quoteIds);
}
