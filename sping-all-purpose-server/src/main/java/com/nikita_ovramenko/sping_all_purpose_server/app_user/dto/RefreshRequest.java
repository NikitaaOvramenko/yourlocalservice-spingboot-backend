package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
    @Override
    public String toString() { return "RefreshRequest[token=REDACTED]"; }
}
