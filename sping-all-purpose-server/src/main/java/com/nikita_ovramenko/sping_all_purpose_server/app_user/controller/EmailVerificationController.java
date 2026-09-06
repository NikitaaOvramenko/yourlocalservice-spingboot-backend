package com.nikita_ovramenko.sping_all_purpose_server.app_user.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.SendVerificationRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.EmailVerificationService;

import jakarta.validation.Valid;

/**
 * Email verification: request a link, and follow it.
 *
 * <p>Both endpoints are public by necessity -- you cannot hold a token before your
 * account is usable, which is the whole point of verifying.
 */
@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    public EmailVerificationController(EmailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Always reports success, whether or not the address has an account.
     *
     * <p>Otherwise this endpoint would answer "does this email have an account here?"
     * for anyone who asks, which is the enumeration leak the login endpoint already
     * avoids.
     */
    @PostMapping("/verify/send")
    public String send(@Valid @RequestBody SendVerificationRequest request) {
        try {
            verificationService.sendVerification(request.email());
        } catch (RuntimeException e) {
            // Unknown address: say nothing different.
        }
        return "If that address has an unverified account, a verification link has been sent.";
    }

    /**
     * Opened from an email client, so the response is plain text a human can read
     * rather than JSON. Failures are handled by GlobalExceptionHandler.
     */
    @GetMapping(value = "/verify/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@PathVariable String token) {
        AppUser user = verificationService.verify(token);
        return "Email " + user.getEmail() + " is verified. You can sign in now.";
    }
}
