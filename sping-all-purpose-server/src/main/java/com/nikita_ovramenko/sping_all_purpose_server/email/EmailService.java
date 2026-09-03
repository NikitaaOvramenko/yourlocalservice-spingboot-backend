package com.nikita_ovramenko.sping_all_purpose_server.email;

import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.file.FileService;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;

/**
 * Submission confirmation to the client, and a notification to the organization.
 *
 * <p>Works from the response snapshot rather than JPA entities, so there is no session,
 * no lazy loading and nothing to detach — which also means it can be tested without a
 * database. Where the mail is sent from comes from the organization's own settings; see
 * {@link OrganizationMailSenderResolver}.
 */
@Service
public class EmailService {

    private final FileService fileService;
    private final OrganizationMailSenderResolver mailSenderResolver;

    public EmailService(FileService fileService, OrganizationMailSenderResolver mailSenderResolver) {
        this.fileService = fileService;
        this.mailSenderResolver = mailSenderResolver;
    }

    public void sendQuoteSubmissionEmails(QuoteResponse quote, Organization organization) {
        ClientSummary client = quote.client();
        String businessInbox = organization.getContactEmail();

        // From has to be an identity the sending account is authorised for, or the
        // provider rejects it and DMARC fails. The resolver returns the account and a
        // matching From together for exactly that reason.
        OrganizationMailSenderResolver.ResolvedSender mailer = mailSenderResolver.resolve(organization);

        SimpleMailMessage toClient = new SimpleMailMessage();
        toClient.setFrom(mailer.from());
        toClient.setReplyTo(businessInbox);
        toClient.setTo(client.email());
        toClient.setSubject("Your Appointment Request Has Been Received");
        toClient.setText(clientBody(quote, organization));

        SimpleMailMessage toBusiness = new SimpleMailMessage();
        toBusiness.setFrom(mailer.from());
        toBusiness.setReplyTo(client.email());
        toBusiness.setTo(businessInbox);
        toBusiness.setSubject("New Appointment Request Submitted");
        toBusiness.setText(businessBody(quote, organization));

        // One call, so both go over a single SMTP connection and a failure on one is
        // reported without preventing the other.
        mailer.sender().send(toClient, toBusiness);
    }

    private String clientBody(QuoteResponse quote, Organization organization) {
        ClientSummary client = quote.client();
        return "Hi " + client.firstName() + ",\n\n"
                + "Thank you for submitting your appointment request.\n"
                + "Here are the details we received:\n\n"
                + "Full Name: " + fullName(client) + "\n"
                + "Email: " + client.email() + "\n"
                + "Phone: " + orEmpty(client.phone()) + "\n\n"
                + "Requested Service Information:\n"
                + "• Organization: " + organization.getName() + "\n"
                + serviceLines(quote)
                + "• Description: " + orEmpty(quote.description()) + "\n\n"
                + locationBlock(quote.location()) + "\n"
                + "We will contact you shortly to confirm the final appointment time.\n\n"
                + "Best regards,\n"
                // The name is the brand as shown on its own site ("YourLocalPaints", "TCS"),
                // so it is used verbatim rather than prefixed.
                + organization.getName();
    }

    private String businessBody(QuoteResponse quote, Organization organization) {
        ClientSummary client = quote.client();
        return "A new client has submitted an appointment request.\n\n"
                + "Client Information:\n"
                + "• Name: " + fullName(client) + "\n"
                + "• Email: " + client.email() + "\n"
                + "• Phone: " + orEmpty(client.phone()) + "\n\n"
                + "Requested Work Details:\n"
                + "• Organization: " + organization.getName() + "\n"
                + serviceLines(quote)
                + "• Description: " + orEmpty(quote.description()) + "\n\n"
                + locationBlock(quote.location()) + "\n"
                + "images: " + presignedPictureLinks(quote) + "\n";
    }

    private static String locationBlock(LocationSummary location) {
        return "Location Details:\n"
                + "• Country: " + location.country() + "\n"
                + "• Province/State: " + orEmpty(location.provinceState()) + "\n"
                + "• Town/City: " + location.city() + "\n"
                + "• Street: " + location.street() + "\n"
                + "• Postal Code: " + location.postalCode() + "\n";
    }

    private static String serviceLines(QuoteResponse quote) {
        StringBuilder lines = new StringBuilder();
        for (QuoteLineItemResponse item : quote.services()) {
            lines.append("• Service: ").append(item.serviceName());
            if (item.quantity() != null && item.quantity() > 1) {
                lines.append(" x").append(item.quantity());
            }
            if (item.description() != null && !item.description().isBlank()) {
                lines.append(" (").append(item.description()).append(")");
            }
            lines.append("\n");
        }
        return lines.toString();
    }

    private List<String> presignedPictureLinks(QuoteResponse quote) {
        return quote.pictureKeys().stream()
                .map(fileService::createPresignedGetLink)
                .toList();
    }

    private static String fullName(ClientSummary client) {
        return client.firstName() + " " + client.lastName();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
