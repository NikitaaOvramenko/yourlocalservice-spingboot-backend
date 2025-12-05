package com.nikita_ovramenko.sping_all_purpose_server.email;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

public record FormDto(String name, String lastname, String email, String phone, String workType, String service,
        String country, String town, String street, String postal_code, String description) {

}
