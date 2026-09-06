package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A catalogue entry.
 *
 * <p>Note the catalogue is global: two organizations can offer the same row, so editing
 * a description here changes it on every site that offers it.
 */
public record ServiceOfferingRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                message = "must be lowercase letters, digits and single hyphens") String slug,
        String description,
        Boolean active) {
}
