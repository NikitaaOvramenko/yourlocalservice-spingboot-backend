package com.nikita_ovramenko.sping_all_purpose_server.quote.event;

import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;

/**
 * Published when a quote has been saved. Handled after the transaction commits.
 *
 * <p>Carries a finished snapshot rather than the {@code Quote} entity. The listener runs
 * once the persistence context is closed, so an entity would arrive detached and any
 * lazy association would blow up on first access. {@link QuoteResponse} is already
 * fully materialised — the mapper builds it inside the transaction — so the email has
 * everything it needs without going back to the database.
 *
 * <p>{@link Organization} is safe to carry for the same reason: it is deliberately made
 * up of basic fields and one embedded value object, with no collections.
 */
public record QuoteSubmittedEvent(QuoteResponse quote, Organization organization) {
}
