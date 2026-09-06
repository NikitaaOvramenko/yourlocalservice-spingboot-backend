package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.common.dto.LineTotals;
import com.nikita_ovramenko.sping_all_purpose_server.common.dto.PageResponse;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.Specs;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteSummary;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quote.exception.QuoteNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quote.mapper.QuoteMapper;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.repository.QuoteLineItemRepo;

/**
 * Reads and edits existing quotes on behalf of staff.
 *
 * <p>Creation lives in {@link QuoteSubmissionService} rather than here, so the two ways
 * a quote can come into existence stay side by side and the difference between them --
 * whether the client gets an email -- is visible in one file.
 */
@Service
public class QuoteAdminService {

    private final QuoteRepo quoteRepo;
    private final QuoteLineItemRepo lineItemRepo;
    private final QuoteMapper quoteMapper;

    public QuoteAdminService(QuoteRepo quoteRepo, QuoteLineItemRepo lineItemRepo, QuoteMapper quoteMapper) {
        this.quoteRepo = quoteRepo;
        this.lineItemRepo = lineItemRepo;
        this.quoteMapper = quoteMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<QuoteSummary> list(String organizationSlug, QuoteStatus status,
            String clientEmail, Pageable pageable) {

        Specification<Quote> spec = Specs.allOfNonNull(
                QuoteSpecifications.fetchSummaryAssociations(),
                QuoteSpecifications.organizationSlug(organizationSlug),
                QuoteSpecifications.status(status),
                QuoteSpecifications.clientEmail(clientEmail));

        Page<Quote> page = quoteRepo.findAll(spec, pageable);
        Map<Long, LineTotals> totals = loadTotals(page.getContent());

        return PageResponse.of(page, quote -> toSummary(quote, totals.get(quote.getId())));
    }

    @Transactional(readOnly = true)
    public QuoteResponse get(Long id) {
        return quoteMapper.toResponse(require(id));
    }

    /** Partial update: a null field is left unchanged. */
    @Transactional
    public QuoteResponse update(Long id, QuoteUpdateRequest request) {
        Quote quote = require(id);

        if (request.status() != null) {
            quote.setStatus(request.status());
        }
        if (request.description() != null) {
            quote.setDescription(request.description());
        }
        if (request.expiresAt() != null) {
            quote.setExpiresAt(request.expiresAt());
        }

        return quoteMapper.toResponse(quoteRepo.save(quote));
    }

    Quote require(Long id) {
        return quoteRepo.findById(id).orElseThrow(() -> new QuoteNotFoundException(id));
    }

    /** One grouped query for the whole page, rather than touching items on each row. */
    private Map<Long, LineTotals> loadTotals(List<Quote> quotes) {
        if (quotes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = quotes.stream().map(Quote::getId).toList();
        return lineItemRepo.findTotalsByQuoteIds(ids).stream()
                .collect(Collectors.toMap(LineTotals::getOwnerId, Function.identity()));
    }

    private static QuoteSummary toSummary(Quote quote, LineTotals totals) {
        // A quote with no line items has no row in the grouped result at all.
        int itemCount = totals == null ? 0 : (int) totals.getItemCount();
        BigDecimal total = totals == null ? null : totals.getTotal();

        return new QuoteSummary(
                quote.getId(),
                quote.getOrganization().getSlug(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                quote.getClient().fullName(),
                quote.getClient().getEmail(),
                quote.getLocation().getCity(),
                itemCount,
                total);
    }
}
