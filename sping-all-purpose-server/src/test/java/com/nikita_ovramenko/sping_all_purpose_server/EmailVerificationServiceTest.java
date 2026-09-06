package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenExpiredException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenInvalidException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.EmailVerification;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.EmailVerificationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AppUserService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.EmailVerificationService;

/** Plain unit test -- no Spring context, no database. */
class EmailVerificationServiceTest {

    private JavaMailSender mailSender;
    private EmailVerificationRepo verificationRepo;
    private AppUserRepo userRepository;
    private EmailVerificationService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setEmail("staff@example.com");
        user.setName("New Staff");
        user.setRole(Role.MEMBER);
        user.setVerified(false);

        mailSender = mock(JavaMailSender.class);
        verificationRepo = mock(EmailVerificationRepo.class);
        userRepository = mock(AppUserRepo.class);

        AppUserService userService = mock(AppUserService.class);
        given(userService.findUserByEmail(anyString())).willReturn(user);

        given(verificationRepo.findByUserId(1L)).willReturn(List.of());
        given(verificationRepo.save(any(EmailVerification.class)))
                .willAnswer(call -> call.getArgument(0));

        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("noreply@example.com");

        service = new EmailVerificationService(
                mailSender, mailProperties, verificationRepo, userRepository, userService);
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.example.com");
        ReflectionTestUtils.setField(service, "ttlMinutes", 60L);
    }

    private EmailVerification token(Instant expiresAt) {
        EmailVerification verification = new EmailVerification();
        verification.setToken("tok-123");
        verification.setUser(user);
        verification.setCreatedAt(Instant.now());
        verification.setExpiresAt(expiresAt);
        return verification;
    }

    @Test
    void sendingIssuesATokenAndEmailsTheLink() {
        service.sendVerification("staff@example.com");

        ArgumentCaptor<EmailVerification> saved = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepo).save(saved.capture());
        assertThat(saved.getValue().getToken()).isNotBlank();
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("staff@example.com");
        assertThat(sent.getValue().getText())
                .contains("https://api.example.com/api/auth/verify/" + saved.getValue().getToken());
    }

    /** Registration calls this straight after creating the account; SMTP being down must not undo it. */
    @Test
    void aFailedSendDoesNotPropagate() {
        willThrow(new MailSendException("smtp down")).given(mailSender).send(any(SimpleMailMessage.class));

        service.sendVerification("staff@example.com");

        verify(verificationRepo).save(any(EmailVerification.class));
    }

    @Test
    void alreadyVerifiedUserGetsNoNewToken() {
        user.setVerified(true);

        service.sendVerification("staff@example.com");

        verify(verificationRepo, never()).save(any(EmailVerification.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /** Asking for a new link must invalidate the previous one, not add a second. */
    @Test
    void issuingANewTokenClearsTheOldOne() {
        EmailVerification existing = token(Instant.now().plus(1, ChronoUnit.HOURS));
        given(verificationRepo.findByUserId(1L)).willReturn(List.of(existing));

        service.sendVerification("staff@example.com");

        verify(verificationRepo).deleteAll(List.of(existing));
    }

    @Test
    void followingAValidLinkVerifiesTheUserAndConsumesTheToken() {
        EmailVerification valid = token(Instant.now().plus(1, ChronoUnit.HOURS));
        given(verificationRepo.findByToken("tok-123")).willReturn(Optional.of(valid));

        AppUser verified = service.verify("tok-123");

        assertThat(verified.isVerified()).isTrue();
        verify(userRepository).save(user);
        verify(verificationRepo).delete(valid);
    }

    /**
     * Regression: the delete happens before the throw, and a RuntimeException from a
     * transactional method rolls the transaction back. Without noRollbackFor on the
     * method, expired tokens survive forever.
     */
    @Test
    void expiredLinkIsRejectedAndTheDeadTokenDeleted() {
        EmailVerification expired = token(Instant.now().minus(1, ChronoUnit.HOURS));
        given(verificationRepo.findByToken("tok-123")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verify("tok-123"))
                .isInstanceOf(VerificationTokenExpiredException.class);

        verify(verificationRepo).delete(expired);
        assertThat(user.isVerified()).isFalse();
    }

    @Test
    void unknownTokenIsRejected() {
        given(verificationRepo.findByToken("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("nope"))
                .isInstanceOf(VerificationTokenInvalidException.class);
    }
}
