package com.nikita_ovramenko.sping_all_purpose_server.organization.dto;

/**
 * An organization as staff see it, including its mail configuration.
 *
 * <p>smtpPasswordEnv is the environment variable name, not the password, so this is safe
 * to return. Nothing here should ever expose a resolved credential.
 */
public record OrganizationDetail(
        Long id,
        String name,
        String slug,
        String contactEmail,
        boolean active,
        boolean mailConfigured,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPasswordEnv,
        Boolean smtpSslEnabled,
        Boolean smtpStarttlsEnabled,
        String fromEmail,
        String fromName) {
}
