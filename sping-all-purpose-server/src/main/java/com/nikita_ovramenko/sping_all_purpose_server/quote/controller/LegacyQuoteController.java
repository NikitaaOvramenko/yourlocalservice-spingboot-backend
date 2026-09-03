package com.nikita_ovramenko.sping_all_purpose_server.quote.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.LegacyQuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.LegacyQuoteAdapter;

/**
 * Deprecated submission endpoint kept for the currently deployed frontends.
 *
 * <p>The path and JSON shape are unchanged; LegacyQuoteAdapter maps the payload onto
 * the current model and calls the same service as POST /api/orgs/{slug}/quotes, which
 * new clients should use instead.
 */
@RestController
@RequestMapping("/api")
public class LegacyQuoteController {

    private final LegacyQuoteAdapter legacyQuoteAdapter;

    public LegacyQuoteController(LegacyQuoteAdapter legacyQuoteAdapter) {
        this.legacyQuoteAdapter = legacyQuoteAdapter;
    }

    @Deprecated
    @PostMapping("/email/form")
    public ResponseEntity<LegacyQuoteResponse> submit(@RequestBody QuoteDto quoteDto) {
        legacyQuoteAdapter.submit(quoteDto);
        // "to" is the client's own email. It previously returned their last name, because
        // the old mapper put getLastname() in the email slot of the DTO this read from.
        //
        // The message no longer claims the email was sent: mail now goes out after this
        // response, on another thread, and may still fail.
        return ResponseEntity.ok(new LegacyQuoteResponse(quoteDto.email(), "Request received"));
    }
}
