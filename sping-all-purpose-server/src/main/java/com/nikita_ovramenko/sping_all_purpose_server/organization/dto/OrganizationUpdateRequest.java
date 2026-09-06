package com.nikita_ovramenko.sping_all_purpose_server.organization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial update; a null field is left unchanged.
 *
 * <p>slug is absent deliberately. It is embedded in public URLs, in already-uploaded S3
 * object keys, and in whatever the four front-ends have hard-coded, so renaming one is
 * a migration rather than an edit.
 *
 * <p>Mail fields are merged with the existing settings, then validated together.
 */
public record OrganizationUpdateRequest(
        @Size(max = 150) String name,
        @Email @Size(max = 254) String contactEmail,
        Boolean active,
        @JsonProperty("mailSettings")
        @JsonAlias("mail") @Valid MailSettingsRequest mail) {
}
