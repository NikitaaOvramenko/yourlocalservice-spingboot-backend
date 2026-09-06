package com.nikita_ovramenko.sping_all_purpose_server.app_user.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AuthSession;

public interface AuthSessionRepo extends JpaRepository<AuthSession, UUID> {
    // Serialize refresh and logout for a session, including across backend instances.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.id = :id")
    Optional<AuthSession> lockById(@Param("id") UUID id);

    @Query("select count(s) > 0 from AuthSession s where s.id = :id "
            + "and lower(s.user.email) = lower(:email) and s.expiresAt > :now")
    boolean isActive(@Param("id") UUID id, @Param("email") String email, @Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
