package com.nikita_ovramenko.sping_all_purpose_server.email;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.file.FileService;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;

/**
 * Submission confirmation to the client, and a notification to the organization.
 *
 * <p>Routing comes from organization.contact_email, replacing the hardcoded
 * workType-to-address map that returned null for anything it did not know about. The
 * sending account comes from the org's own mail settings where it has them, so mail is
 * signed by the domain it claims to be from.
 */
@Service
public class EmailService {

    private final FileService fileService;
    private final OrganizationMailSenderResolver mailSenderResolver;

    public EmailService(FileService fileService, OrganizationMailSenderResolver mailSenderResolver) {
        this.fileService = fileService;
        this.mailSenderResolver = mailSenderResolver;
    }

    public EmailDto sendQuoteSubmissionEmails(Quote quote) {
        Client client = quote.getClient();
        Location location = quote.getLocation();
        Organization organization = quote.getOrganization();

        String businessInbox = organization.getContactEmail();
        // From has to be an identity the sending account is authorised for, or the
        // provider rejects it and DMARC fails. The resolver returns the account and a
        // matching From together for exactly that reason.
        OrganizationMailSenderResolver.ResolvedSender mailer = mailSenderResolver.resolve(organization);
        String from = mailer.from();

        // The organization name is the brand as shown on its own site ("YourLocalPaints",
        // "TCS"), so it is used verbatim. Prefixing "YourLocal" the way the old
        // workType-based code did would sign the mail "YourLocalYourLocalPaints".
        String signature = organization.getName();
        String serviceLines = formatServices(quote);
        String locationBlock = "Location Details:\n"
                + "• Country: " + location.getCountry() + "\n"
                + "• Province/State: " + location.getProvinceState() + "\n"
                + "• Town/City: " + location.getCity() + "\n"
                + "• Street: " + location.getStreet() + "\n"
                + "• Postal Code: " + location.getPostalCode() + "\n";

        SimpleMailMessage mailForCustomer = new SimpleMailMessage();
        mailForCustomer.setFrom(from);
        mailForCustomer.setReplyTo(businessInbox);
        mailForCustomer.setTo(client.getEmail());
        mailForCustomer.setSubject("Your Appointment Request Has Been Received");
        mailForCustomer.setText(
                "Hi " + client.getFirstName() + ",\n\n"
                        + "Thank you for submitting your appointment request.\n"
                        + "Here are the details we received:\n\n"
                        + "Full Name: " + client.fullName() + "\n"
                        + "Email: " + client.getEmail() + "\n"
                        + "Phone: " + client.getPhone() + "\n\n"
                        + "Requested Service Information:\n"
                        + "• Organization: " + organization.getName() + "\n"
                        + serviceLines
                        + "• Description: " + nullToEmpty(quote.getDescription()) + "\n\n"
                        + locationBlock + "\n"
                        + "We will contact you shortly to confirm the final appointment time.\n\n"
                        + "Best regards,\n"
                        + signature);

        mailer.sender().send(mailForCustomer);

        SimpleMailMessage mailForBusiness = new SimpleMailMessage();
        mailForBusiness.setFrom(from);
        mailForBusiness.setReplyTo(client.getEmail());
        mailForBusiness.setTo(businessInbox);
        mailForBusiness.setSubject("New Appointment Request Submitted");
        mailForBusiness.setText(
                "A new client has submitted an appointment request.\n\n"
                        + "Client Information:\n"
                        + "• Name: " + client.fullName() + "\n"
                        + "• Email: " + client.getEmail() + "\n"
                        + "• Phone: " + client.getPhone() + "\n\n"
                        + "Requested Work Details:\n"
                        + "• Organization: " + organization.getName() + "\n"
                        + serviceLines
                        + "• Description: " + nullToEmpty(quote.getDescription()) + "\n\n"
                        + locationBlock + "\n"
                        + "images: " + presignedPictureLinks(quote) + "\n");

        mailer.sender().send(mailForBusiness);

        return new EmailDto(client.getEmail(), "Emails sent successfully.");
    }

    private static String formatServices(Quote quote) {
        return quote.getItems().stream()
                .map(EmailService::formatService)
                .collect(Collectors.joining());
    }

    private static String formatService(QuoteLineItem item) {
        String line = "• Service: " + item.getService().getName();
        if (item.getQuantity() != null && item.getQuantity() > 1) {
            line += " x" + item.getQuantity();
        }
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            line += " (" + item.getDescription() + ")";
        }
        return line + "\n";
    }

    private List<String> presignedPictureLinks(Quote quote) {
        List<String> links = new ArrayList<>();
        for (String objectKey : quote.getPictures()) {
            links.add(fileService.createPresignedGetLink(objectKey));
        }
        return links;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
