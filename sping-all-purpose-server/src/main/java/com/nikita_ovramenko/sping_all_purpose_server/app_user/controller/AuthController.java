package com.nikita_ovramenko.sping_all_purpose_server.app_user.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AppUserResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AuthResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.LoginRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.RefreshRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.RegisterRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper.AppUserMapper;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AppUserService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AuthService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.EmailVerificationService;

import jakarta.validation.Valid;

/**
 * Registration, login, token refresh, and "who am I".
 *
 * <p>These are the only endpoints under /api/auth that are reachable without a token --
 * see SecurityConfig, where everything not explicitly listed requires authentication.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppUserService userService;
    private final AppUserMapper userMapper;
    private final EmailVerificationService verificationService;

    public AuthController(AuthService authService, AppUserService userService,
            AppUserMapper userMapper, EmailVerificationService verificationService) {
        this.authService = authService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.verificationService = verificationService;
    }

    /**
     * Creates an unverified account and emails a verification link.
     *
     * <p>The two steps are separate calls on purpose: the account is committed by
     * registerUser before any SMTP work starts, so a mail failure cannot roll the
     * registration back. The user can request another link if it does not arrive.
     */
    @PostMapping("/register")
    public ResponseEntity<AppUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUser created = userService.registerUser(request);
        verificationService.sendVerification(created.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(created));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** The refresh token identifies the session, so logout works after access expiry. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Requires a valid token; the principal's name is the email JwtFilter authenticated. */
    @GetMapping("/me")
    public AppUserResponse me(Principal principal) {
        return userMapper.toResponse(userService.findUserByEmail(principal.getName()));
    }
}
