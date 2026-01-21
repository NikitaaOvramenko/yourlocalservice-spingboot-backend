package com.nikita_ovramenko.sping_all_purpose_server.email;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
public class EmailController {

    private final QuoteService quoteService;

    public EmailController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/email/form")
    public ResponseEntity<EmailDto> quoteSubmission(@RequestBody QuoteDto quoteDto) {
        QuoteDto quoteDto2 = quoteService.save(quoteDto);
        EmailDto emailDto = new EmailDto(quoteDto2.email(), "Email Sent Successfully !");
        return ResponseEntity.ok(emailDto);
    }

}
