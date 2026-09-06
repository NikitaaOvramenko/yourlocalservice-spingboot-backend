package com.nikita_ovramenko.sping_all_purpose_server.app_user.service;

import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;
import java.time.Instant;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import io.jsonwebtoken.JwtException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AuthResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.LoginRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotVerifiedException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserWrongPasswordException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper.AppUserMapper;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AuthSession;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AuthSessionRepo;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.JwtService;

@Service
public class AuthService {

    private final AppUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppUserMapper userMapper;
    private final AuthSessionRepo sessions;

    public AuthService(AppUserService userService, PasswordEncoder passwordEncoder,
            JwtService jwtService, AppUserMapper userMapper, AuthSessionRepo sessions) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.sessions = sessions;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userService.findUserByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UserWrongPasswordException("Incorrect email or password");
        }
        if (!user.isVerified()) {
            throw new UserNotVerifiedException("Account " + user.getEmail() + " is not verified");
        }

        sessions.deleteByExpiresAtBefore(Instant.now());
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        // Absolute session lifetime: refresh does not extend a login indefinitely.
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        return issueTokens(user, session);
    }

    /**
     * Exchanges a refresh token for a fresh pair.
     *
     * <p>The user is re-read rather than trusted from the token, so an account deleted
     * or unverified since the token was issued stops working immediately.
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        JwtService.TokenIdentity identity = jwtService.readRefreshToken(refreshToken);
        AuthSession session = sessions.lockById(identity.sessionId())
                .orElseThrow(() -> new JwtException("Session expired or revoked"));
        if (!session.getExpiresAt().isAfter(Instant.now())
                || !session.getUser().getEmail().equalsIgnoreCase(identity.email())
                || !MessageDigest.isEqual(session.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8),
                        hash(refreshToken).getBytes(StandardCharsets.UTF_8))) {
            throw new JwtException("Invalid or already used refresh token");
        }
        AppUser user = userService.findUserByEmail(identity.email());

        if (!user.isVerified()) {
            throw new UserNotVerifiedException("Account " + user.getEmail() + " is not verified");
        }
        return issueTokens(user, session);
    }

    @Transactional
    public void logout(String refreshToken) {
        JwtService.TokenIdentity identity = jwtService.readRefreshToken(refreshToken);
        // Any signed refresh token for this session can revoke it, even if a refresh
        // just rotated it. This makes logout safe when it races a refresh request.
        sessions.lockById(identity.sessionId()).ifPresent(session -> {
            if (!session.getUser().getEmail().equalsIgnoreCase(identity.email())) {
                throw new JwtException("Invalid session owner");
            }
            sessions.delete(session);
        });
    }

    private AuthResponse issueTokens(AppUser user, AuthSession session) {
        String accessToken = jwtService.generateToken(
                user.getEmail(), Map.of("role", user.getRole().name(), "sid", session.getId().toString()));
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), session.getId(), session.getExpiresAt());
        session.setRefreshTokenHash(hash(refreshToken));
        sessions.save(session);

        return new AuthResponse(accessToken, refreshToken, userMapper.toResponse(user));
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JVM", e);
        }
    }
}
