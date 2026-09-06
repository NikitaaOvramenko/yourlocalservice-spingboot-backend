package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AuthSession;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AuthSessionRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AuthResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.LoginRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotVerifiedException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserWrongPasswordException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper.AppUserMapper;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AppUserService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AuthService;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.JwtService;

/** Plain unit test -- no Spring context, no database. */
class AuthServiceTest {

    private static final String PASSWORD = "correct-horse-battery";

    private AppUserService userService;
    private JwtService jwtService;
    private AuthService authService;
    private AppUser user;
    private AuthSessionRepo sessions;
    private AuthSession session;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        user = new AppUser();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setName("Administrator");
        user.setRole(Role.ADMIN);
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setVerified(true);

        userService = mock(AppUserService.class);
        given(userService.findUserByEmail(anyString())).willReturn(user);

        jwtService = mock(JwtService.class);
        given(jwtService.generateToken(anyString(), org.mockito.ArgumentMatchers.any()))
                .willReturn("access-token");
        given(jwtService.generateRefreshToken(anyString(), any(), any())).willReturn("refresh-token");

        sessions = mock(AuthSessionRepo.class);
        session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        try {
            session.setRefreshTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest("some-refresh-token".getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) { throw new AssertionError(e); }
        given(sessions.lockById(session.getId())).willReturn(Optional.of(session));
        given(jwtService.readRefreshToken("some-refresh-token"))
                .willReturn(new JwtService.TokenIdentity(user.getEmail(), session.getId()));
        authService = new AuthService(userService, encoder, jwtService, new AppUserMapper(), sessions);
    }

    @Test
    void correctCredentialsIssueBothTokensAndTheUser() {
        AuthResponse response = authService.login(new LoginRequest("admin@example.com", PASSWORD));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("admin@example.com");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", "wrong")))
                .isInstanceOf(UserWrongPasswordException.class);
    }

    /** Registration creates unverified users, so this is the normal post-signup state. */
    @Test
    void unverifiedAccountCannotLogInEvenWithTheRightPassword() {
        user.setVerified(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", PASSWORD)))
                .isInstanceOf(UserNotVerifiedException.class);
    }

    @Test
    void unknownUserIsRejected() {
        willThrow(new UserNotFoundException("nope")).given(userService).findUserByEmail("ghost@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", PASSWORD)))
                .isInstanceOf(UserNotFoundException.class);
    }

    /**
     * Refresh re-reads the user rather than trusting the token, so an account that has
     * been unverified since the token was issued stops working immediately.
     */
    @Test
    void refreshRejectsAnAccountUnverifiedSinceTheTokenWasIssued() {
        user.setVerified(false);

        assertThatThrownBy(() -> authService.refresh("some-refresh-token"))
                .isInstanceOf(UserNotVerifiedException.class);
    }

    @Test
    void refreshIssuesAFreshPair() {
        assertThat(authService.refresh("some-refresh-token").accessToken()).isEqualTo("access-token");
    }

    @Test
    void refreshReplacesTheStoredHashAndCannotBeReused() {
        authService.refresh("some-refresh-token");
        assertThat(session.getRefreshTokenHash()).hasSize(64).doesNotContain("refresh-token");
        assertThatThrownBy(() -> authService.refresh("some-refresh-token"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void logoutCanRevokeASessionAfterItsRefreshTokenRotates() {
        authService.refresh("some-refresh-token");
        authService.logout("some-refresh-token");
        verify(sessions).delete(session);
    }

    @Test
    void missingOrExpiredSessionCannotRefresh() {
        session.setExpiresAt(Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> authService.refresh("some-refresh-token"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
        given(sessions.lockById(session.getId())).willReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refresh("some-refresh-token"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    /** The response type must never be able to carry the hash. */
    @Test
    void responseCarriesNoPasswordHash() {
        AuthResponse response = authService.login(new LoginRequest("admin@example.com", PASSWORD));

        assertThat(response.user().toString()).doesNotContain(user.getPasswordHash());
        assertThat(AuthResponse.class.getRecordComponents()).noneMatch(c -> c.getName().contains("password"));
        assertThat(response.toString()).doesNotContain("access-token", "refresh-token");
        assertThat(new LoginRequest("admin@example.com", PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(new com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.RefreshRequest("secret-token").toString())
                .doesNotContain("secret-token");
    }
}
