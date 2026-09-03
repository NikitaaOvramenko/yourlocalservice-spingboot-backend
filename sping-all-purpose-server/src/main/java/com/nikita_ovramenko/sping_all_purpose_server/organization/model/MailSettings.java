package com.nikita_ovramenko.sping_all_purpose_server.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How one organization sends mail. Embedded in {@link Organization}, so these are
 * columns on the organization table rather than a separate provider table -- an org
 * sends through exactly one account at a time, so there is nothing to be one-to-many
 * with.
 *
 * <p>All fields are nullable and all-or-nothing (enforced by ck_organization_smtp_complete):
 * when {@link #isConfigured()} is false the org falls back to the application-wide
 * spring.mail.* sender.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class MailSettings {

    @Column(name = "smtp_host", length = 255)
    private String host;

    @Column(name = "smtp_port")
    private Integer port;

    @Column(name = "smtp_username", length = 254)
    private String username;

    /**
     * The NAME of the environment variable holding the SMTP password -- never the
     * password itself. Keeping the secret out of the database means backups and
     * database dumps carry no credentials.
     */
    @Column(name = "smtp_password_env", length = 100)
    private String passwordEnv;

    @Column(name = "smtp_ssl_enabled")
    private Boolean sslEnabled;

    @Column(name = "smtp_starttls_enabled")
    private Boolean starttlsEnabled;

    /** The address recipients see. Must be one this SMTP account is allowed to send as. */
    @Column(name = "from_email", length = 254)
    private String fromEmail;

    @Column(name = "from_name", length = 150)
    private String fromName;

    public boolean isConfigured() {
        return host != null && !host.isBlank();
    }

    /** "YourLocalPaints &lt;info@example.co&gt;", or the bare address when no name is set. */
    public String formattedFrom() {
        if (fromName == null || fromName.isBlank()) {
            return fromEmail;
        }
        return fromName + " <" + fromEmail + ">";
    }
}
