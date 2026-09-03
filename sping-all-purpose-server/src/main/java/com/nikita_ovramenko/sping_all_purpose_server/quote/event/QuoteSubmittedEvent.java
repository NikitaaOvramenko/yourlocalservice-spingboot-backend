package com.nikita_ovramenko.sping_all_purpose_server.quote.event;

/**
 * Published after a quote is saved. Carries only the id: the listener runs after the
 * transaction commits, by which point the original persistence context is gone and
 * touching a lazy association on a passed entity would throw.
 */
public record QuoteSubmittedEvent(Long quoteId) {
}
