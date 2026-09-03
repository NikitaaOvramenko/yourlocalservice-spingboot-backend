package com.nikita_ovramenko.sping_all_purpose_server.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.file.FileService;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Submission confirmation to the client, and a notification to the organization.
 *
 * <p>Works from the response snapshot rather than JPA entities, so there is no session,
 * no lazy loading and nothing to detach -- which also means it can be tested without a
 * database. Where the mail is sent from comes from the organization's own settings; see
 * {@link OrganizationMailSenderResolver}.
 */
@Service
public class EmailService {

    /**
     * Raw photo bytes attached to one notification.
     *
     * <p>MIME base64-encodes attachments, inflating them by about a third, so this
     * arrives as roughly 7MB on the wire -- comfortably inside every provider's limit.
     * The cap is really about heap: emailTaskExecutor runs up to 4 sends at once, and
     * each holds its bytes plus an encoded copy. Anything over budget is linked instead.
     */
    private static final long MAX_INLINE_BYTES = 5L * 1024 * 1024;

    private final FileService fileService;
    private final OrganizationMailSenderResolver mailSenderResolver;

    public EmailService(FileService fileService, OrganizationMailSenderResolver mailSenderResolver) {
        this.fileService = fileService;
        this.mailSenderResolver = mailSenderResolver;
    }

    public void sendQuoteSubmissionEmails(QuoteResponse quote, Organization organization)
            throws MessagingException {

        ClientSummary client = quote.client();
        String businessInbox = organization.getContactEmail();
        OrganizationMailSenderResolver.ResolvedSender mailer = mailSenderResolver.resolve(organization);

        Photos photos = collectPhotos(quote.pictureKeys());

        // The client's copy is plain text and carries no photos -- they sent them.
        MimeMessage toClient = mailer.sender().createMimeMessage();
        MimeMessageHelper clientHelper = new MimeMessageHelper(toClient, false, "UTF-8");
        clientHelper.setFrom(mailer.from());
        clientHelper.setReplyTo(businessInbox);
        clientHelper.setTo(client.email());
        clientHelper.setSubject("Your Appointment Request Has Been Received");
        clientHelper.setText(clientBody(quote, organization));

        MimeMessage toBusiness = mailer.sender().createMimeMessage();
        MimeMessageHelper businessHelper = new MimeMessageHelper(toBusiness, true, "UTF-8");
        businessHelper.setFrom(mailer.from());
        businessHelper.setReplyTo(client.email());
        businessHelper.setTo(businessInbox);
        businessHelper.setSubject("New Appointment Request Submitted");

        // Plain-text alternative keeps the mail usable where HTML is blocked; it lists
        // every photo as a link, since plain text cannot show an inline image.
        businessHelper.setText(
                businessBody(quote, organization, quote.pictureKeys()),
                businessBodyHtml(quote, organization, photos));

        // MUST come after setText: the helper attaches inline parts to the body part,
        // which does not exist yet beforehand. Reversed, images silently do not render.
        for (int i = 0; i < photos.attached().size(); i++) {
            FileService.StoredObject photo = photos.attached().get(i);
            businessHelper.addInline("photo" + i, new ByteArrayResource(photo.bytes()),
                    photo.contentType() == null ? "application/octet-stream" : photo.contentType());
        }

        mailer.sender().send(toClient, toBusiness);
    }

    /** What was pulled down to attach, and what has to stay a link. */
    private record Photos(List<FileService.StoredObject> attached, List<String> linked) {
    }

    /**
     * Sizes every photo first and only downloads the ones that fit, so a quote with
     * thirty photos does not pull thirty files down to attach three.
     */
    private Photos collectPhotos(List<String> keys) {
        List<FileService.StoredObject> attached = new ArrayList<>();
        List<String> linked = new ArrayList<>();
        long budget = MAX_INLINE_BYTES;

        for (String key : keys) {
            OptionalLong size = fileService.contentLength(key);
            if (size.isEmpty() || size.getAsLong() > budget) {
                linked.add(key);
                continue;
            }
            Optional<FileService.StoredObject> object = fileService.getObject(key);
            if (object.isEmpty()) {
                linked.add(key);
                continue;
            }
            budget -= object.get().bytes().length;
            attached.add(object.get());
        }
        return new Photos(attached, linked);
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

    private String businessBody(QuoteResponse quote, Organization organization, List<String> photoKeys) {
        ClientSummary client = quote.client();
        StringBuilder body = new StringBuilder()
                .append("A new client has submitted an appointment request.\n\n")
                .append("Client Information:\n")
                .append("• Name: ").append(fullName(client)).append("\n")
                .append("• Email: ").append(client.email()).append("\n")
                .append("• Phone: ").append(orEmpty(client.phone())).append("\n\n")
                .append("Requested Work Details:\n")
                .append("• Organization: ").append(organization.getName()).append("\n")
                .append(serviceLines(quote))
                .append("• Description: ").append(orEmpty(quote.description())).append("\n\n")
                .append(locationBlock(quote.location())).append("\n");

        if (photoKeys.isEmpty()) {
            return body.append("Photos: none submitted\n").toString();
        }
        body.append("Photos (links valid for 7 days):\n");
        for (String key : photoKeys) {
            body.append("• ").append(fileService.createPresignedGetLink(key)).append("\n");
        }
        return body.toString();
    }

    private String businessBodyHtml(QuoteResponse quote, Organization organization, Photos photos) {
        StringBuilder html = new StringBuilder("<html><body style=\"font-family:sans-serif\">")
                .append("<pre style=\"font-family:sans-serif;font-size:14px\">")
                .append(HtmlUtils.htmlEscape(businessBodyWithoutPhotos(quote, organization)))
                .append("</pre>");

        for (int i = 0; i < photos.attached().size(); i++) {
            html.append("<img src=\"cid:photo").append(i)
                    .append("\" style=\"max-width:600px;margin:8px 0;display:block\">");
        }

        if (!photos.linked().isEmpty()) {
            html.append("<p>Remaining photos (links valid for 7 days):</p><ul>");
            for (String key : photos.linked()) {
                html.append("<li><a href=\"").append(HtmlUtils.htmlEscape(fileService.createPresignedGetLink(key)))
                        .append("\">").append(HtmlUtils.htmlEscape(key)).append("</a></li>");
            }
            html.append("</ul>");
        }
        return html.append("</body></html>").toString();
    }

    /** The HTML version renders photos itself, so its text block leaves them out. */
    private String businessBodyWithoutPhotos(QuoteResponse quote, Organization organization) {
        return businessBody(quote, organization, List.of()).replace("Photos: none submitted\n", "");
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

    private static String fullName(ClientSummary client) {
        return client.firstName() + " " + client.lastName();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
