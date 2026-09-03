package com.nikita_ovramenko.sping_all_purpose_server.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of the "YourLocal[Service]" sites. Identified in URLs by {@link #slug}.
 *
 * <p>Replaces the free-text Quote.workType string and the hardcoded workTypeToEmail
 * map that used to live in EmailService. contact_email is the business inbox that
 * used to be a value in that map.
 */
@Entity
@Table(name = "organization")
@Getter
@Setter
@NoArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    /** URL key, e.g. "junk-removal" in /api/orgs/junk-removal/quotes. */
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    /** Business inbox for this org. Used as To/Reply-To, never as From. */
    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * This org's own SMTP account, or null/unconfigured to use the application-wide
     * spring.mail.* sender. Embedded, so these live as columns on this table.
     */
    @Embedded
    private MailSettings mailSettings = new MailSettings();
}
