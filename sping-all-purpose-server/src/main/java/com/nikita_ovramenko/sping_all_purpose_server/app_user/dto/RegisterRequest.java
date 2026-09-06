package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String name,
        /** Minimum 8: bcrypt silently ignores anything past 72 bytes, so that is the ceiling. */
        @NotBlank @Size(min = 8, max = 72) String password) {
}
