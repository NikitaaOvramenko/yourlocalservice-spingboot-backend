package com.nikita_ovramenko.sping_all_purpose_server.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * How one organization sends mail.
 *
 * <p>All-or-nothing: either host is null (the org falls back to the application-wide
 * sender) or host, port, username, passwordEnv and fromEmail are all present. The
 * database enforces this too, via ck_organization_smtp_complete, but the service checks
 * it first so a partial update is a 400 naming the missing fields rather than an opaque
 * constraint violation.
 *
 * <p>passwordEnv is the NAME of an environment variable, never a password. The secret
 * stays out of the database so backups and dumps carry no credentials.
 */
public record MailSettingsRequest(
        @Size(max = 255) String host,
        @Min(1) @Max(65535) Integer port,
        @Size(max = 254) String username,
        @Size(max = 100) String passwordEnv,
        Boolean sslEnabled,
        Boolean starttlsEnabled,
        @Email @Size(max = 254) String fromEmail,
        @Size(max = 150) String fromName) {
}
