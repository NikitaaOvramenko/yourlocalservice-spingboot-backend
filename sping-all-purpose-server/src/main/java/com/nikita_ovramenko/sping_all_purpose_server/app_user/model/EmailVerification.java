package com.nikita_ovramenko.sping_all_purpose_server.app_user.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single-use token emailed to a user to prove they own the address.
 *
 * <p>Consumed on success and deleted on expiry, so the table only ever holds tokens
 * that are still outstanding.
 *
 * <p>There is deliberately no stored is_expired flag: expiry is a function of
 * expiresAt and the current time, and a persisted copy can only ever disagree with it.
 */
@Entity
@Table(name = "email_verification",
        indexes = @Index(name = "ix_email_verification_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Random UUID; this is what travels in the emailed link. */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
