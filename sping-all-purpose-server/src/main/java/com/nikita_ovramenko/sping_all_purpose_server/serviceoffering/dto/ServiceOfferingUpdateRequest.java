package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto;

import jakarta.validation.constraints.Size;

/** Partial update; a null field is left unchanged. slug is not changeable. */
public record ServiceOfferingUpdateRequest(
        @Size(max = 150) String name,
        String description,
        Boolean active) {
}
