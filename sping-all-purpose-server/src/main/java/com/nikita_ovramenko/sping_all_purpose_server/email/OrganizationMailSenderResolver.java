package com.nikita_ovramenko.sping_all_purpose_server.email;

import java.util.Properties;

import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.organization.model.MailSettings;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;

/**
 * Chooses which SMTP account an organization's mail goes out through, and what From
 * address it carries.
 *
 * <p>An organization with no mail settings uses the application-wide sender, which is
 * exactly the previous behaviour. One with its own settings gets a sender built from
 * them, so its mail is actually sent by the domain it claims to come from.
 */
@Component
public class OrganizationMailSenderResolver {

    private static final int SOCKET_TIMEOUT_MS = 10_000;

    private final JavaMailSender defaultSender;
    private final MailProperties defaultMailProperties;
    private final Environment environment;

    public OrganizationMailSenderResolver(JavaMailSender defaultSender,
            MailProperties defaultMailProperties, Environment environment) {
        this.defaultSender = defaultSender;
        this.defaultMailProperties = defaultMailProperties;
        this.environment = environment;
    }

    /** A sender paired with the From address it is allowed to use. */
    public record ResolvedSender(JavaMailSender sender, String from) {
    }

    public ResolvedSender resolve(Organization organization) {
        MailSettings settings = organization.getMailSettings();

        // Hibernate returns a null embeddable when every column is null, so both the
        // holder and the configured flag have to be checked.
        if (settings == null || !settings.isConfigured()) {
            return new ResolvedSender(defaultSender, defaultMailProperties.getUsername());
        }

        return new ResolvedSender(build(organization, settings), settings.formattedFrom());
    }

    /**
     * Built per send rather than cached. JavaMailSenderImpl holds no connection -- it
     * opens a Transport per message -- so construction is cheap, and building fresh
     * means a settings change in the database takes effect immediately with no cache
     * to invalidate.
     */
    private JavaMailSenderImpl build(Organization organization, MailSettings settings) {
        String password = environment.getProperty(settings.getPasswordEnv());
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Organization '" + organization.getSlug() + "' is configured to send via "
                            + settings.getHost() + " but the environment variable '"
                            + settings.getPasswordEnv() + "' holding its SMTP password is not set");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getHost());
        sender.setPort(settings.getPort());
        sender.setUsername(settings.getUsername());
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", String.valueOf(Boolean.TRUE.equals(settings.getSslEnabled())));
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(settings.getStarttlsEnabled())));

        // Jakarta Mail defaults every socket timeout to INFINITE. Without these, a host
        // that accepts the connection and then goes silent occupies a pool thread
        // forever rather than failing and freeing it.
        props.put("mail.smtp.connectiontimeout", String.valueOf(SOCKET_TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(SOCKET_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(SOCKET_TIMEOUT_MS));
        return sender;
    }
}
