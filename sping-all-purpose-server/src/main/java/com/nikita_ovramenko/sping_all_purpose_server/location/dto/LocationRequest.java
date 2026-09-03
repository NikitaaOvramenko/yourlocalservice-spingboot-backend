package com.nikita_ovramenko.sping_all_purpose_server.location.dto;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocationRequest(
        @NotNull Country country,
        @Size(max = 100) String provinceState,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String postalCode) {
}
