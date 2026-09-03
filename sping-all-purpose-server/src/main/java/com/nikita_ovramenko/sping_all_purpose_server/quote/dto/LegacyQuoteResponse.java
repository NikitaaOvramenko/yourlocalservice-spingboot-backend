package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

/**
 * Response body of the deprecated POST /api/email/form.
 *
 * <p>Field names are the original wire contract and must not change while any frontend
 * still posts to that endpoint.
 */
public record LegacyQuoteResponse(String to, String message) {
}
