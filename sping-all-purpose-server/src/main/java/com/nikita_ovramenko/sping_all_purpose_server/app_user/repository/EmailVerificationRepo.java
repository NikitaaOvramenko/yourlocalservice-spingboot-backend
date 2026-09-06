package com.nikita_ovramenko.sping_all_purpose_server.app_user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.EmailVerification;

@Repository
public interface EmailVerificationRepo extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByToken(String token);

    /**
     * A user's outstanding tokens. Used to clear the old one before issuing a new link,
     * via deleteAll -- a derived delete would need its own @Transactional, and the
     * caller deliberately runs without one so SMTP never happens inside a transaction.
     */
    List<EmailVerification> findByUserId(Long userId);
}
