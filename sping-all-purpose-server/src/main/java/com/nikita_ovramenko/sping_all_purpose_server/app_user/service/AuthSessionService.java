package com.nikita_ovramenko.sping_all_purpose_server.app_user.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AuthSessionRepo;
import io.jsonwebtoken.JwtException;

@Service
public class AuthSessionService {
    private final AuthSessionRepo sessions;

    public AuthSessionService(AuthSessionRepo sessions) {
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public void requireActive(UUID sessionId, String email) {
        if (!sessions.isActive(sessionId, email, Instant.now())) {
            throw new JwtException("Session expired or revoked");
        }
    }
}
