package com.nikita_ovramenko.sping_all_purpose_server.common.service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${my.app.jwt-key}")
    private String SECRET_KEY;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public record TokenIdentity(String email, UUID sessionId) { }

    public TokenIdentity readAccessToken(String token) {
        return readToken(token, "access");
    }

    public TokenIdentity readRefreshToken(String token) {
        return readToken(token, "refresh");
    }

    private TokenIdentity readToken(String token, String expectedType) {
        Claims claims = extractAllClaims(token);
        if (!expectedType.equals(claims.get("tokenUse", String.class))
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null) {
            throw new JwtException("Invalid token purpose or claims");
        }
        try {
            return new TokenIdentity(claims.getSubject(), UUID.fromString(claims.get("sid", String.class)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JwtException("Invalid session claim", e);
        }
    }

    public String generateToken(String username, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .claim("tokenUse", "access")
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, UUID sessionId, Instant expiresAt) {
        return Jwts.builder()
                .claim("tokenUse", "refresh")
                .claim("sid", sessionId.toString())
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(Date.from(expiresAt))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
