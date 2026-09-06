package com.nikita_ovramenko.sping_all_purpose_server.job.dto;

import java.time.Instant;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Creates a job one of two ways. The service rejects any mixture of the two rather than
 * silently ignoring the fields that do not apply.
 *
 * <p><b>From a quote</b> -- send only quoteId, optionally with scheduledAt, status and
 * description. Client, organization, location and every line with its price are copied
 * off the quote. This is the accepted-quote-becomes-work step.
 *
 * <p><b>Walk-in</b> -- send organizationSlug, client, location and services, leaving
 * quoteId null. job.quote_id is nullable precisely so work that never had a quote can
 * still be tracked.
 *
 * <p>status defaults to SCHEDULED when omitted.
 */
public record JobCreateRequest(
        Long quoteId,
        @JsonProperty("orgSlug") @JsonAlias("organizationSlug") String organizationSlug,
        @Valid ClientRequest client,
        @Valid LocationRequest location,
        @Size(max = 50) List<@NotNull @Valid JobLineItemCreateRequest> services,
        @Size(max = 4000) String description,
        Instant scheduledAt,
        JobStatus status) {

    /** True when the caller means "turn this quote into a job". */
    public boolean fromQuote() {
        return quoteId != null;
    }
}
