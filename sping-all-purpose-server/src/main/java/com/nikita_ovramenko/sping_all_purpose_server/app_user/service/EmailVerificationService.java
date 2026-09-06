package com.nikita_ovramenko.sping_all_purpose_server.app_user.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenExpiredException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenInvalidException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.EmailVerification;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.EmailVerificationRepo;

/**
 * Issues and consumes email verification tokens.
 *
 * <p>A token is a random UUID stored against the user with an expiry. The link goes out
 * by mail; following it marks the user verified and deletes the token, so every token
 * is single-use.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailVerificationRepo verificationRepo;
    private final AppUserRepo userRepository;
    private final AppUserService userService;

    /** Where the emailed link points. Must be the public URL, not localhost, in production. */
    @Value("${spring.app.verification_base_url}")
    private String baseUrl;

    @Value("${spring.app.verification_ttl_minutes}")
    private long ttlMinutes;

    public EmailVerificationService(JavaMailSender mailSender, MailProperties mailProperties,
            EmailVerificationRepo verificationRepo, AppUserRepo userRepository, AppUserService userService) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.verificationRepo = verificationRepo;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Issues a token and emails the link.
     *
     * <p>Deliberately not @Transactional. Each repository call carries its own
     * transaction, so the token is committed before any SMTP work begins -- a link can
     * never arrive for a token that was not stored, and a slow mail server does not hold
     * a database connection open while it times out.
     *
     * <p>The send failure is swallowed on purpose: the caller has usually just created
     * an account, and failing that because SMTP is down would throw the registration
     * away. The user can ask for another link.
     */
    public void sendVerification(String email) {
        AppUser user = userService.findUserByEmail(email);

        if (user.isVerified()) {
            log.info("Verification requested for {}, which is already verified", email);
            return;
        }

        // One live token per user: asking for a new link invalidates the previous one.
        verificationRepo.deleteAll(verificationRepo.findByUserId(user.getId()));

        EmailVerification verification = new EmailVerification();
        verification.setToken(UUID.randomUUID().toString());
        verification.setUser(user);
        verification.setCreatedAt(Instant.now());
        verification.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
        String token = verificationRepo.save(verification).getToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(user.getEmail());
        message.setSubject("Verify your email address");
        message.setText("Hi " + user.getName() + ",\n\n"
                + "Confirm your email address by opening this link:\n\n"
                + baseUrl + "/api/auth/verify/" + token + "\n\n"
                + "The link is valid for " + ttlMinutes + " minutes. If you did not create "
                + "an account, you can ignore this message.\n");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Could not send verification email to {}", user.getEmail(), e);
        }
    }

    /**
     * Consumes a token: marks the user verified and deletes it.
     *
     * <p>An expired token is deleted too -- it is of no further use, and leaving it
     * would let the table fill with dead rows.
     *
     * <p>noRollbackFor is load-bearing: a RuntimeException thrown from a transactional
     * method rolls the transaction back, which would undo the delete immediately above
     * the throw and leave the dead token in the table forever.
     */
    @Transactional(noRollbackFor = VerificationTokenExpiredException.class)
    public AppUser verify(String token) {
        EmailVerification verification = verificationRepo.findByToken(token)
                .orElseThrow(() -> new VerificationTokenInvalidException(
                        "This verification link is not valid. It may already have been used."));

        if (verification.isExpired()) {
            verificationRepo.delete(verification);
            throw new VerificationTokenExpiredException(
                    "This verification link has expired. Request a new one.");
        }

        AppUser user = verification.getUser();
        user.setVerified(true);
        userRepository.save(user);
        verificationRepo.delete(verification);

        log.info("Verified email for user {}", user.getEmail());
        return user;
    }
}
