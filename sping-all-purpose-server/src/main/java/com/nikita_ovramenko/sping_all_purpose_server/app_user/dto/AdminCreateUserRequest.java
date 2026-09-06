package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An administrator creating an account directly, rather than the person self-registering.
 *
 * <p>The 72-character password ceiling is BCrypt's, not an arbitrary limit: bytes beyond
 * 72 are silently ignored by the algorithm, so accepting a longer password would give a
 * false impression of strength.
 *
 * <p>verified defaults to true when omitted: an admin creating an account has already
 * established who the person is, and there is no reason to make them chase an email --
 * particularly while the shared SMTP sender is unreliable.
 */
public record AdminCreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Boolean verified) {
}
