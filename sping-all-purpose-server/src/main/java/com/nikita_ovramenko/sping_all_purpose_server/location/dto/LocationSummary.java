package com.nikita_ovramenko.sping_all_purpose_server.location.dto;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

public record LocationSummary(
        Long id, Country country, String provinceState, String city, String street, String postalCode) {
}
