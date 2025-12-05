package com.nikita_ovramenko.sping_all_purpose_server.location.dto;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

public record LocationDto(Country country, String town, String street, String postalCode, Long clientId) {

}
