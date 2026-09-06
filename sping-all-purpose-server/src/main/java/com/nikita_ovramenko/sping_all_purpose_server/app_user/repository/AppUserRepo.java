package com.nikita_ovramenko.sping_all_purpose_server.app_user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    /**
     * IgnoreCase because email is case-insensitive in practice, and Optional to match
     * the rest of the codebase -- a null return is easy to forget to check.
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Backs the guard that stops the last administrator being demoted or deleted.
     * There is no password-reset flow, so losing every admin is unrecoverable in-app.
     */
    long countByRole(Role role);
}
