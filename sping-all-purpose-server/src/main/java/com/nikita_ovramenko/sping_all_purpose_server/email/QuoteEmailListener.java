package com.nikita_ovramenko.sping_all_purpose_server.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nikita_ovramenko.sping_all_purpose_server.quote.event.QuoteSubmittedEvent;

/**
 * Sends the submission emails once the quote is durably saved.
 *
 * <p>AFTER_COMMIT: the row is committed before any mail is attempted, so an SMTP
 * failure can no longer roll back the client's quote — it can only cost the
 * notification, which is why the failure is logged rather than rethrown.
 *
 * <p>@Async: the two SMTP round-trips happen on a pool thread, so the client gets their
 * 201 without waiting for them.
 *
 * <p>No transaction and no repository here: the event already carries everything the
 * email needs.
 */
@Component
public class QuoteEmailListener {

    private static final Logger log = LoggerFactory.getLogger(QuoteEmailListener.class);

    private final EmailService emailService;

    public QuoteEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuoteSubmitted(QuoteSubmittedEvent event) {
        try {
            emailService.sendQuoteSubmissionEmails(event.quote(), event.organization());
        } catch (Exception e) {
            log.error("Failed to send submission emails for quote {}", event.quote().id(), e);
        }
    }
}
