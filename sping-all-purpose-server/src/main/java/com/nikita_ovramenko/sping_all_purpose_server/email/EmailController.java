package com.nikita_ovramenko.sping_all_purpose_server.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.LegacyQuoteAdapter;

/**
 * Deprecated submission endpoint kept for the currently deployed frontends.
 *
 * <p>The wire format is unchanged; LegacyQuoteAdapter maps it onto the new model and
 * calls the same submission service as POST /api/orgs/{slug}/quotes. New clients
 * should use that instead.
 */
@RestController
@RequestMapping("/api")
public class EmailController {

    private final LegacyQuoteAdapter legacyQuoteAdapter;

    public EmailController(LegacyQuoteAdapter legacyQuoteAdapter) {
        this.legacyQuoteAdapter = legacyQuoteAdapter;
    }

    @Deprecated
    @PostMapping("/email/form")
    public ResponseEntity<EmailDto> quoteSubmission(@RequestBody QuoteDto quoteDto) {
        legacyQuoteAdapter.submit(quoteDto);
        // The client's own email, not (as previously) their last name: the old mapper
        // put getLastname() in the email slot of the DTO this was read from.
        return ResponseEntity.ok(new EmailDto(quoteDto.email(), "Email Sent Successfully !"));
    }
}
