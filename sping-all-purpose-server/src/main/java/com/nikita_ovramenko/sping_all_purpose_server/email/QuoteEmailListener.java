package com.nikita_ovramenko.sping_all_purpose_server.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nikita_ovramenko.sping_all_purpose_server.quote.event.QuoteSubmittedEvent;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;

/**
 * Sends the submission emails once the quote is durably saved.
 *
 * <p>Previously the send happened inside the submission transaction, so an SMTP
 * timeout rolled back the client's quote. AFTER_COMMIT means the row is committed
 * first and a mail failure can only cost the notification.
 *
 * <p>Not @Async: the response still waits on SMTP, exactly as before. Decoupling that
 * latency is a separate change and needs a bounded executor to go with it.
 */
@Component
public class QuoteEmailListener {

    private static final Logger log = LoggerFactory.getLogger(QuoteEmailListener.class);

    private final QuoteRepo quoteRepo;
    private final EmailService emailService;

    public QuoteEmailListener(QuoteRepo quoteRepo, EmailService emailService) {
        this.quoteRepo = quoteRepo;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onQuoteSubmitted(QuoteSubmittedEvent event) {
        try {
            // Re-read rather than carrying the entity on the event: the submitting
            // persistence context is closed by now, so its lazy associations are dead.
            Quote quote = quoteRepo.findWithDetailsById(event.quoteId()).orElse(null);
            if (quote == null) {
                log.error("Quote {} vanished before its confirmation email could be sent", event.quoteId());
                return;
            }
            emailService.sendQuoteSubmissionEmails(quote);
        } catch (Exception e) {
            // The quote is already committed; never let a mail failure escape and
            // surface to the client as a failed submission.
            log.error("Failed to send submission emails for quote {}", event.quoteId(), e);
        }
    }
}
