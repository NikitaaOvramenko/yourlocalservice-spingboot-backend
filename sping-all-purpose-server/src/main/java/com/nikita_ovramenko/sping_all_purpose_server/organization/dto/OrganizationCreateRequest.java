package com.nikita_ovramenko.sping_all_purpose_server.organization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new organization.
 *
 * <p>The slug pattern matches what the public routes assume: it appears in
 * /api/orgs/{slug}/... and in S3 object keys, so anything outside lowercase letters,
 * digits and hyphens would need escaping somewhere and eventually not get it.
 */
public record OrganizationCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                message = "must be lowercase letters, digits and single hyphens") String slug,
        @NotBlank @Email @Size(max = 254) String contactEmail,
        Boolean active,
        @JsonProperty("mailSettings")
        @JsonAlias("mail") @Valid MailSettingsRequest mail) {
}
