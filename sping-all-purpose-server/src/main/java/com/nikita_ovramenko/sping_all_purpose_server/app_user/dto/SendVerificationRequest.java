package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationRequest(@NotBlank @Email String email) {
}
