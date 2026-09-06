package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Staff entering a lead that arrived by phone or in person.
 *
 * <p>Wraps the public QuoteRequest rather than restating it, so the two paths cannot
 * drift apart. The organization moves into the body because this endpoint is not
 * nested under /api/orgs/{slug}.
 */
public record AdminQuoteCreateRequest(
        @NotBlank @JsonProperty("orgSlug") @JsonAlias("organizationSlug") String organizationSlug,
        @NotNull @Valid QuoteRequest quote) {
}
