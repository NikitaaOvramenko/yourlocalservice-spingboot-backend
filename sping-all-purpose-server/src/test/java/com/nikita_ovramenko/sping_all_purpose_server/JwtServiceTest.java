package com.nikita_ovramenko.sping_all_purpose_server;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.JwtService;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {
    private final JwtService jwt = new JwtService();
    private final UUID sid = UUID.randomUUID();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(jwt, "SECRET_KEY", "test-jwt-signing-key-that-is-long-enough-for-hs256");
    }

    @Test
    void tokensAreBoundToTheirPurposeAndSession() {
        String access = jwt.generateToken("admin@example.com", Map.of("sid", sid.toString()));
        String refresh = jwt.generateRefreshToken("admin@example.com", sid, Instant.now().plusSeconds(3600));
        assertThat(jwt.readAccessToken(access).sessionId()).isEqualTo(sid);
        assertThat(jwt.readRefreshToken(refresh).email()).isEqualTo("admin@example.com");
        assertThatThrownBy(() -> jwt.readAccessToken(refresh)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.readRefreshToken(access)).isInstanceOf(JwtException.class);
    }

    @Test
    void everyRefreshTokenIsUniqueEvenInTheSameSecond() {
        Instant expiry = Instant.now().plusSeconds(3600);
        assertThat(jwt.generateRefreshToken("admin@example.com", sid, expiry))
                .isNotEqualTo(jwt.generateRefreshToken("admin@example.com", sid, expiry));
    }

    @Test
    void expiredMalformedTamperedAndMissingSessionTokensAreRejected() {
        String expired = jwt.generateRefreshToken("admin@example.com", sid, Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> jwt.readRefreshToken(expired)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.readRefreshToken("not-a-token")).isInstanceOf(JwtException.class);
        String access = jwt.generateToken("admin@example.com", Map.of());
        assertThatThrownBy(() -> jwt.readAccessToken(access)).isInstanceOf(JwtException.class);
        String valid = jwt.generateToken("admin@example.com", Map.of("sid", sid.toString()));
        String[] pieces = valid.split("\\.");
        String tampered = pieces[0] + "." + pieces[1] + "." + (pieces[2].startsWith("a") ? "b" : "a") + pieces[2].substring(1);
        assertThatThrownBy(() -> jwt.readAccessToken(tampered)).isInstanceOf(JwtException.class);
    }
}
