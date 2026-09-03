package com.nikita_ovramenko.sping_all_purpose_server.quote.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteSubmissionService;

import jakarta.validation.Valid;

/**
 * Quote submission, scoped to one organization by the {slug} path segment.
 *
 * <p>There is deliberately no GET /quotes/{id}. Security is currently permitAll, so a
 * read endpoint keyed on a sequential id would let anyone walk 1, 2, 3... and harvest
 * every client's name, email, phone and street address. A token-scoped read belongs
 * with the authentication work.
 */
@RestController
@RequestMapping("/api/orgs/{slug}/quotes")
public class QuoteController {

    private final QuoteSubmissionService quoteSubmissionService;

    public QuoteController(QuoteSubmissionService quoteSubmissionService) {
        this.quoteSubmissionService = quoteSubmissionService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> submit(
            @PathVariable String slug,
            @Valid @RequestBody QuoteRequest request) {

        QuoteResponse response = quoteSubmissionService.submit(slug, request);
        URI location = URI.create("/api/orgs/" + slug + "/quotes/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
