package com.nikita_ovramenko.sping_all_purpose_server.quote.enums;

/**
 * Lifecycle of a quote request.
 *
 * <p>Replaces the previous BEGAN/IN_PROGRESS/COMPLETED, which described work being
 * performed rather than a quote being priced, sent and answered -- and had no way to
 * express the expires_at column.
 *
 * <p>Nothing currently transitions a quote to EXPIRED; expiry is computed on read
 * (SENT with expiresAt in the past). A sweeper is deliberately out of scope.
 */
public enum QuoteStatus {
    /** Submitted by the client, not yet priced. */
    SUBMITTED,
    /** Priced and sent back to the client. */
    SENT,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
