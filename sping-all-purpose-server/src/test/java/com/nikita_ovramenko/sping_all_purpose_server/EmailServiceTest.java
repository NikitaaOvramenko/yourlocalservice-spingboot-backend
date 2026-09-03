package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.email.EmailService;
import com.nikita_ovramenko.sping_all_purpose_server.email.OrganizationMailSenderResolver;
import com.nikita_ovramenko.sping_all_purpose_server.file.FileService;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;

/**
 * Plain unit test -- no Spring context, no database, no container.
 *
 * <p>That is possible because EmailService works from the response snapshot rather than
 * JPA entities. It previously needed a live Postgres purely to have a session open for
 * lazy loading.
 */
class EmailServiceTest {

    private JavaMailSender sender;
    private EmailService emailService;
    private Organization organization;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        FileService fileService = mock(FileService.class);
        given(fileService.createPresignedGetLink(anyString()))
                .willAnswer(call -> "https://signed/" + call.getArgument(0));

        OrganizationMailSenderResolver resolver = mock(OrganizationMailSenderResolver.class);
        given(resolver.resolve(org.mockito.ArgumentMatchers.any()))
                .willReturn(new OrganizationMailSenderResolver.ResolvedSender(
                        sender, "TCS <tcs.ontario@gmail.com>"));

        emailService = new EmailService(fileService, resolver);

        organization = new Organization();
        organization.setName("TCS");
        organization.setSlug("tcs");
        organization.setContactEmail("tcs.ontario@gmail.com");
    }

    private static QuoteResponse quote(List<String> pictureKeys) {
        return new QuoteResponse(
                7L, "tcs", QuoteStatus.SUBMITTED, Instant.now(), null,
                new ClientSummary(1L, "Nikita", "O", "client@example.com", "+16478097778"),
                new LocationSummary(1L, Country.CANADA, "ON", "Toronto", "12 Bay St", "L4K0A1"),
                List.of(new QuoteLineItemResponse(1L, 13L, "Waterproofing", "waterproofing",
                        null, 2, null, "basement")),
                "Testing", pictureKeys);
    }

    private List<SimpleMailMessage> capture() {
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture(), sent.capture());
        return sent.getAllValues();
    }

    @Test
    void sendsClientConfirmationAndBusinessNotificationTogether() {
        emailService.sendQuoteSubmissionEmails(quote(List.of()), organization);

        List<SimpleMailMessage> sent = capture();
        assertThat(sent).hasSize(2);

        SimpleMailMessage toClient = sent.get(0);
        assertThat(toClient.getTo()).containsExactly("client@example.com");
        assertThat(toClient.getReplyTo()).isEqualTo("tcs.ontario@gmail.com");

        SimpleMailMessage toBusiness = sent.get(1);
        assertThat(toBusiness.getTo()).containsExactly("tcs.ontario@gmail.com");
        assertThat(toBusiness.getReplyTo()).isEqualTo("client@example.com");

        // From must be the sending identity for both, never the recipient's address.
        assertThat(toClient.getFrom()).isEqualTo("TCS <tcs.ontario@gmail.com>");
        assertThat(toBusiness.getFrom()).isEqualTo("TCS <tcs.ontario@gmail.com>");
    }

    /**
     * Regression: the photo links live only in the business notification, and building
     * them used to fail after the client's mail had already been sent.
     */
    @Test
    void businessNotificationCarriesPresignedPhotoLinks() {
        emailService.sendQuoteSubmissionEmails(
                quote(List.of("quotes/a/photo1.jpg", "quotes/a/photo2.jpg")), organization);

        assertThat(capture().get(1).getText())
                .contains("https://signed/quotes/a/photo1.jpg")
                .contains("https://signed/quotes/a/photo2.jpg");
    }

    @Test
    void serviceLinesShowNameAndQuantity() {
        emailService.sendQuoteSubmissionEmails(quote(List.of()), organization);

        assertThat(capture().get(0).getText())
                .contains("• Service: Waterproofing x2 (basement)")
                .contains("• Organization: TCS");
    }

    /** The org name is the brand already, so it must not be prefixed with "YourLocal". */
    @Test
    void signatureUsesTheOrganizationNameVerbatim() {
        emailService.sendQuoteSubmissionEmails(quote(List.of()), organization);

        assertThat(capture().get(0).getText())
                .endsWith("Best regards,\nTCS")
                .doesNotContain("YourLocalTCS");
    }
}
