-- Per-organization outbound mail settings.
--
-- Motivation: every organization's mail currently goes out with From set to the single
-- authenticated SMTP account (spring.mail.username), with the org's own address only in
-- Reply-To. Under DMARC that is the pattern that gets spam-foldered or rejected -- and
-- it is unfixable for TCS, whose address is on gmail.com rather than a domain we sign.
--
-- Every column is nullable and they are all-or-nothing: an organization with
-- smtp_host NULL keeps using the global spring.mail.* sender exactly as before, so this
-- migration changes no behaviour on its own. Populate a row to move that one org onto
-- its own sender.
--
-- smtp_password_env holds the NAME of an environment variable, never the password
-- itself: the secret stays in the environment so database backups do not become
-- credential dumps. The existing three orgs can point at the SMTP_PASS that is already
-- set; a new org needs its variable added to the deployment.

ALTER TABLE organization
    ADD COLUMN smtp_host             VARCHAR(255),
    ADD COLUMN smtp_port             INTEGER,
    ADD COLUMN smtp_username         VARCHAR(254),
    ADD COLUMN smtp_password_env     VARCHAR(100),
    ADD COLUMN smtp_ssl_enabled      BOOLEAN,
    ADD COLUMN smtp_starttls_enabled BOOLEAN,
    ADD COLUMN from_email            VARCHAR(254),
    ADD COLUMN from_name             VARCHAR(150);

-- Half-configured mail is worse than none: it fails at send time, after the quote has
-- already been committed and the client told we received it. Reject it at write time.
ALTER TABLE organization
    ADD CONSTRAINT ck_organization_smtp_complete CHECK (
        smtp_host IS NULL
        OR (smtp_port IS NOT NULL
            AND smtp_username IS NOT NULL
            AND smtp_password_env IS NOT NULL
            AND from_email IS NOT NULL)
    );

-- Example: move TCS onto its own Gmail sender so its mail is actually signed by the
-- domain it claims. Requires an app password (not the account password) exposed as
-- SMTP_PASS_TCS in the deployment environment. Left commented out because the value
-- has to be provisioned first.
--
-- UPDATE organization SET
--     smtp_host             = 'smtp.gmail.com',
--     smtp_port             = 587,
--     smtp_username         = 'tcs.ontario@gmail.com',
--     smtp_password_env     = 'SMTP_PASS_TCS',
--     smtp_ssl_enabled      = FALSE,
--     smtp_starttls_enabled = TRUE,
--     from_email            = 'tcs.ontario@gmail.com',
--     from_name             = 'TCS'
-- WHERE slug = 'tcs';
