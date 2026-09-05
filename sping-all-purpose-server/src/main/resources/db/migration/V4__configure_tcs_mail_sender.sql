-- Point TCS at its own Gmail account.
--
-- Its address is on gmail.com, a domain we cannot DKIM-sign, so sending it through the
-- shared neo.space account meant mail claiming an identity it was not authorised for --
-- the pattern DMARC exists to reject. This makes the From match the sending account.
--
-- Nothing here is environment-specific: host, port, username and From are identical in
-- every environment, and smtp_password_env holds the NAME of an environment variable,
-- never a secret. That is what makes this safe as a migration rather than a manual
-- statement someone has to remember after every database reset.
--
-- !! Requires SMTP_PASS_TCS to be set in the deployment environment !!
-- It must be a Google *app password* (2-Step Verification has to be on for the account),
-- not the account password. Without it, TCS's business notification fails and is logged;
-- quotes are unaffected, because mail is sent after the transaction commits.
--
-- The other three organizations deliberately keep smtp_host NULL: they share the
-- yourlocalservice.co mailbox and use the application-wide spring.mail.* sender.

UPDATE organization SET
    smtp_host             = 'smtp.gmail.com',
    smtp_port             = 587,
    smtp_username         = 'tcs.ontario@gmail.com',
    smtp_password_env     = 'SMTP_PASS_TCS',
    smtp_ssl_enabled      = FALSE,
    smtp_starttls_enabled = TRUE,
    from_email            = 'tcs.ontario@gmail.com',
    from_name             = 'TCS'
WHERE slug = 'tcs';
