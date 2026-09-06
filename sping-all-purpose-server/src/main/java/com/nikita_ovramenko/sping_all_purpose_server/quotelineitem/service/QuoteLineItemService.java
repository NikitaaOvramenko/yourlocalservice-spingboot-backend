package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.exception.QuoteNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.exception.QuoteLineItemNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.repository.QuoteLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * Pricing a quote: the lines are where unitPrice actually gets set, which until now was
 * impossible through the API at all.
 *
 * <p>Every method takes the quote id as well as the item id and checks that the item
 * really belongs to that quote. Without it, /quotes/1/items/999 would happily edit a
 * line on quote 7 -- the nested URL would be decoration rather than a constraint.
 */
@Service
public class QuoteLineItemService {

    private final QuoteRepo quoteRepo;
    private final QuoteLineItemRepo lineItemRepo;
    private final OfferedServiceResolver offeredServiceResolver;

    public QuoteLineItemService(QuoteRepo quoteRepo, QuoteLineItemRepo lineItemRepo,
            OfferedServiceResolver offeredServiceResolver) {
        this.quoteRepo = quoteRepo;
        this.lineItemRepo = lineItemRepo;
        this.offeredServiceResolver = offeredServiceResolver;
    }

    @Transactional(readOnly = true)
    public List<QuoteLineItemResponse> list(Long quoteId) {
        requireQuote(quoteId);
        return lineItemRepo.findByQuoteIdOrderByIdAsc(quoteId).stream()
                .map(QuoteLineItemService::toResponse)
                .toList();
    }

    @Transactional
    public QuoteLineItemResponse add(Long quoteId, QuoteLineItemCreateRequest request) {
        Quote quote = requireQuote(quoteId);
        ServiceOffering service = offeredServiceResolver.requireOffered(
                quote.getOrganization(), request.serviceId());

        // uq_quote_service forbids the same service twice on one quote. Checked here so
        // the caller is told which service is already present, rather than getting the
        // generic "conflicts with existing data" from the constraint violation.
        if (lineItemRepo.existsByQuoteIdAndServiceId(quoteId, request.serviceId())) {
            throw new ConflictException("Quote " + quoteId + " already has a line for service "
                    + service.getName() + " (id " + service.getId() + "). Update that line instead.");
        }

        QuoteLineItem item = new QuoteLineItem();
        item.setQuote(quote);
        item.setService(service);
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setDescription(request.description());

        return toResponse(lineItemRepo.save(item));
    }

    /** Partial update: a null field is left unchanged. The service is not re-assignable. */
    @Transactional
    public QuoteLineItemResponse update(Long quoteId, Long itemId, QuoteLineItemUpdateRequest request) {
        QuoteLineItem item = requireItemOnQuote(quoteId, itemId);

        if (request.unitPrice() != null) {
            item.setUnitPrice(request.unitPrice());
        }
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }

        return toResponse(lineItemRepo.save(item));
    }

    @Transactional
    public void delete(Long quoteId, Long itemId) {
        lineItemRepo.delete(requireItemOnQuote(quoteId, itemId));
    }

    private Quote requireQuote(Long quoteId) {
        return quoteRepo.findById(quoteId).orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }

    /**
     * An item addressed under the wrong quote is reported as not found, not as a
     * mismatch: the caller has no business knowing that id exists elsewhere.
     */
    private QuoteLineItem requireItemOnQuote(Long quoteId, Long itemId) {
        QuoteLineItem item = lineItemRepo.findById(itemId)
                .orElseThrow(() -> new QuoteLineItemNotFoundException(quoteId, itemId));
        if (!item.getQuote().getId().equals(quoteId)) {
            throw new QuoteLineItemNotFoundException(quoteId, itemId);
        }
        return item;
    }

    private static QuoteLineItemResponse toResponse(QuoteLineItem item) {
        return new QuoteLineItemResponse(
                item.getId(),
                item.getService().getId(),
                item.getService().getName(),
                item.getService().getSlug(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.lineTotal(),
                item.getDescription());
    }
}
