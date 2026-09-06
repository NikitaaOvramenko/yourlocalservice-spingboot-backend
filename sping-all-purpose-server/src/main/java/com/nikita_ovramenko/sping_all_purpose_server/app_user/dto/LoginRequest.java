package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    @Override
    public String toString() { return "LoginRequest[credentials=REDACTED]"; }
}
