package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Plain unit test -- no Spring context, no database, no container.
 *
 * <p>Possible because EmailService works from the response snapshot rather than JPA
 * entities. It previously needed a live Postgres purely to keep a session open for
 * lazy loading.
 */
class EmailServiceTest {

    private static final long ONE_MB = 1024L * 1024L;

    private JavaMailSender sender;
    private FileService fileService;
    private EmailService emailService;
    private Organization organization;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        given(sender.createMimeMessage()).willAnswer(call -> new MimeMessage((jakarta.mail.Session) null));

        fileService = mock(FileService.class);
        given(fileService.createPresignedGetLink(anyString()))
                .willAnswer(call -> "https://signed/" + call.getArgument(0));

        OrganizationMailSenderResolver resolver = mock(OrganizationMailSenderResolver.class);
        given(resolver.resolve(any())).willReturn(
                new OrganizationMailSenderResolver.ResolvedSender(sender, "TCS <tcs.ontario@gmail.com>"));

        emailService = new EmailService(fileService, resolver);

        organization = new Organization();
        organization.setName("TCS");
        organization.setSlug("tcs");
        organization.setContactEmail("tcs.ontario@gmail.com");
    }

    /** Makes a key resolvable at the given size, with matching bytes. */
    private void givenPhoto(String key, long sizeBytes) {
        given(fileService.contentLength(key)).willReturn(OptionalLong.of(sizeBytes));
        given(fileService.getObject(key)).willReturn(Optional.of(
                new FileService.StoredObject(key, new byte[(int) sizeBytes], "image/jpeg")));
    }

    /** Makes a key unreadable, as an object deleted or permission-denied would be. */
    private void givenUnreadablePhoto(String key) {
        given(fileService.contentLength(key)).willReturn(OptionalLong.empty());
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

    private List<MimeMessage> send(List<String> pictureKeys) throws Exception {
        emailService.sendQuoteSubmissionEmails(quote(pictureKeys), organization);
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture(), sent.capture());
        return sent.getAllValues();
    }

    /** Flattens every text part of a message into one string. */
    private static String textOf(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof MimeMultipart multipart) {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                all.append(textOf(multipart.getBodyPart(i))).append("\n");
            }
            return all.toString();
        }
        return "";
    }

    private static List<String> contentIds(Part part) throws Exception {
        List<String> ids = new ArrayList<>();
        if (part.getContent() instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                var body = multipart.getBodyPart(i);
                String[] cid = body.getHeader("Content-ID");
                if (cid != null) {
                    ids.add(cid[0]);
                }
                ids.addAll(contentIds(body));
            }
        }
        return ids;
    }

    @Test
    void sendsClientConfirmationAndBusinessNotificationTogether() throws Exception {
        List<MimeMessage> sent = send(List.of());
        assertThat(sent).hasSize(2);

        MimeMessage toClient = sent.get(0);
        assertThat(toClient.getAllRecipients()[0]).hasToString("client@example.com");
        assertThat(toClient.getReplyTo()[0]).hasToString("tcs.ontario@gmail.com");
        assertThat(toClient.getFrom()[0]).hasToString("TCS <tcs.ontario@gmail.com>");

        MimeMessage toBusiness = sent.get(1);
        assertThat(toBusiness.getAllRecipients()[0]).hasToString("tcs.ontario@gmail.com");
        assertThat(toBusiness.getReplyTo()[0]).hasToString("client@example.com");
        assertThat(toBusiness.getFrom()[0]).hasToString("TCS <tcs.ontario@gmail.com>");
    }

    @Test
    void photosWithinBudgetAreAttachedInline() throws Exception {
        givenPhoto("a.jpg", ONE_MB);
        givenPhoto("b.jpg", ONE_MB);

        MimeMessage toBusiness = send(List.of("a.jpg", "b.jpg")).get(1);

        assertThat(contentIds(toBusiness)).containsExactly("<photo0>", "<photo1>");
        assertThat(textOf(toBusiness)).contains("cid:photo0").contains("cid:photo1");
    }

    /** Over the 5MB budget the rest must fall back to links, not be dropped. */
    @Test
    void photosOverBudgetFallBackToLinks() throws Exception {
        givenPhoto("small.jpg", ONE_MB);
        givenPhoto("huge.jpg", 9 * ONE_MB);

        MimeMessage toBusiness = send(List.of("small.jpg", "huge.jpg")).get(1);

        assertThat(contentIds(toBusiness)).containsExactly("<photo0>");
        assertThat(textOf(toBusiness)).contains("https://signed/huge.jpg");
    }

    /** An oversized photo must be skipped without ever being downloaded. */
    @Test
    void oversizedPhotosAreNeverFetched() throws Exception {
        given(fileService.contentLength("huge.jpg")).willReturn(OptionalLong.of(50 * ONE_MB));

        send(List.of("huge.jpg"));

        verify(fileService, org.mockito.Mockito.never()).getObject("huge.jpg");
    }

    /** One unreadable photo must not cost the business the whole notification. */
    @Test
    void unreadablePhotoDegradesToALinkAndStillSends() throws Exception {
        givenUnreadablePhoto("gone.jpg");

        MimeMessage toBusiness = send(List.of("gone.jpg")).get(1);

        assertThat(contentIds(toBusiness)).isEmpty();
        assertThat(textOf(toBusiness)).contains("https://signed/gone.jpg");
    }

    /** Plain-text readers get links for every photo, since text cannot show an image. */
    @Test
    void plainTextAlternativeListsEveryPhotoAsALink() throws Exception {
        givenPhoto("a.jpg", ONE_MB);

        String body = textOf(send(List.of("a.jpg")).get(1));

        assertThat(body).contains("Photos (links valid for 7 days)")
                .contains("https://signed/a.jpg");
    }

    @Test
    void serviceLinesShowNameAndQuantity() throws Exception {
        assertThat(textOf(send(List.of()).get(0)))
                .contains("• Service: Waterproofing x2 (basement)")
                .contains("• Organization: TCS");
    }

    /** The org name is the brand already, so it must not be prefixed with "YourLocal". */
    @Test
    void signatureUsesTheOrganizationNameVerbatim() throws Exception {
        assertThat(textOf(send(List.of()).get(0)))
                .endsWith("Best regards,\nTCS")
                .doesNotContain("YourLocalTCS");
    }
}
