package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

public record QuoteDto(String name, String lastname, String email, String phone, String workType, List<String> service,
                String country, String town, String street, String postal_code, String description,
                List<String> images) {

}
