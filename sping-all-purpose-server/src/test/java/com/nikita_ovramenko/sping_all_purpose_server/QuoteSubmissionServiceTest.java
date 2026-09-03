package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.repository.LocationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.organization.exception.OrganizationNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteSubmissionService;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;

@SpringBootTest
@ActiveProfiles("test")
class QuoteSubmissionServiceTest extends AbstractPostgresTest {

    @Autowired
    private QuoteSubmissionService quoteSubmissionService;

    @Autowired
    private LocationRepo locationRepo;

    @Autowired
    private QuoteRepo quoteRepo;

    /** Stubbed so tests never attempt a real SMTP connection. */
    @MockitoBean
    private JavaMailSender javaMailSender;

    // Ids from V2__seed_organizations_and_services.sql.
    private static final long FURNITURE_REMOVAL = 21L;
    private static final long PROPERTY_CLEANOUTS = 22L;
    private static final long INTERIOR_PAINTING = 1L;

    private static QuoteRequest request(String email, long serviceId, String postalCode) {
        return new QuoteRequest(
                new ClientRequest("John", "Doe", email, "+14165551234"),
                new LocationRequest(Country.CANADA, "ON", "Toronto", "5 King St", postalCode),
                List.of(new QuoteLineItemRequest(serviceId, 1, "old sofa")),
                "Basement cleanout",
                List.of());
    }

    /**
     * The core bug this refactor fixes: the previous implementation constructed a new
     * Location on every submission, so repeat clients accumulated duplicate addresses.
     */
    @Test
    void submittingTheSameAddressTwiceReusesOneLocationRow() {
        String email = "dedup@example.com";

        QuoteResponse first = quoteSubmissionService.submit("yourlocaljunkremoval", request(email, FURNITURE_REMOVAL, "M5H 1A1"));
        QuoteResponse second = quoteSubmissionService.submit("yourlocaljunkremoval", request(email, PROPERTY_CLEANOUTS, "M5H 1A1"));

        assertThat(first.client().id()).isEqualTo(second.client().id());
        assertThat(first.location().id()).isEqualTo(second.location().id());
        assertThat(locationRepo.findByClientId(first.client().id())).hasSize(1);
    }

    /** Postal code formatting must not defeat the dedup. */
    @Test
    void postalCodeFormattingDoesNotCreateADuplicateLocation() {
        String email = "postal@example.com";

        QuoteResponse first = quoteSubmissionService.submit("yourlocaljunkremoval", request(email, FURNITURE_REMOVAL, "M5H 1A1"));
        QuoteResponse second = quoteSubmissionService.submit("yourlocaljunkremoval", request(email, PROPERTY_CLEANOUTS, "m5h1a1"));

        assertThat(first.location().id()).isEqualTo(second.location().id());
        assertThat(locationRepo.findByClientId(first.client().id())).hasSize(1);
    }

    /**
     * The other core fix: the email send used to run inside the submission transaction,
     * so an SMTP failure discarded the client's quote entirely.
     */
    @Test
    void smtpFailureDoesNotRollBackTheQuote() {
        willThrow(new MailSendException("smtp is down"))
                .given(javaMailSender).send(any(SimpleMailMessage.class));

        QuoteResponse response =
                quoteSubmissionService.submit("yourlocaljunkremoval", request("smtp@example.com", FURNITURE_REMOVAL, "M5H 2A2"));

        assertThat(quoteRepo.findById(response.id())).isPresent();
    }

    @Test
    void unknownOrganizationSlugIsRejected() {
        assertThatThrownBy(() ->
                quoteSubmissionService.submit("does-not-exist", request("x@example.com", FURNITURE_REMOVAL, "M5H 3A3")))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    /** "Interior Painting" is a real service, but it belongs to YourLocalPaints. */
    @Test
    void serviceOfferedByAnotherOrganizationIsRejected() {
        assertThatThrownBy(() ->
                quoteSubmissionService.submit("yourlocaljunkremoval", request("y@example.com", INTERIOR_PAINTING, "M5H 4A4")))
                .isInstanceOf(ServiceNotOfferedException.class);
    }

    @Test
    void serviceThatIsNotInTheCatalogAtAllIsRejected() {
        assertThatThrownBy(() ->
                quoteSubmissionService.submit("yourlocaljunkremoval", request("z@example.com", 9999L, "M5H 5A5")))
                .isInstanceOf(UnknownServiceException.class);
    }
}
