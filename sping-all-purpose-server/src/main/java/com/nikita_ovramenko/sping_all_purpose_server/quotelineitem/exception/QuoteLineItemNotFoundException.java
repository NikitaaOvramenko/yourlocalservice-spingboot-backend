package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

public class QuoteLineItemNotFoundException extends ResourceNotFoundException {

    /**
     * Deliberately says "on quote N" rather than just the item id: the same message is
     * used when the item exists but belongs to a different quote, and treating that as
     * "not found" is what stops /quotes/1/items/{id} being able to edit quote 7's lines.
     */
    public QuoteLineItemNotFoundException(Long quoteId, Long itemId) {
        super("No line item with id " + itemId + " on quote " + quoteId);
    }
}
