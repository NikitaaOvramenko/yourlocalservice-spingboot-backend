package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.service.ClientResolver;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.service.LocationResolver;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.event.QuoteSubmittedEvent;
import com.nikita_ovramenko.sping_all_purpose_server.quote.mapper.QuoteMapper;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteSubmissionService;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

class QuoteSubmissionEventTest {
    private final QuoteRepo quotes = mock(QuoteRepo.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Organization organization = new Organization();
    private final QuoteRequest request = new QuoteRequest(null, null,
            List.of(new QuoteLineItemRequest(11L, 2, "Roof")), "Phone lead", List.of("orgs/tcs/photo.jpg"));
    private QuoteSubmissionService service;

    @BeforeEach
    void setUp() {
        ClientResolver clients = mock(ClientResolver.class);
        LocationResolver locations = mock(LocationResolver.class);
        OrganizationLookup organizations = mock(OrganizationLookup.class);
        OfferedServiceResolver offerings = mock(OfferedServiceResolver.class);
        Client client = new Client();
        when(clients.upsert(null)).thenReturn(client);
        when(locations.resolve(client, null)).thenReturn(new Location());
        when(organizations.requireBySlug("tcs")).thenReturn(organization);
        ServiceOffering roofing = new ServiceOffering();
        roofing.setId(11L);
        when(offerings.requireAllOffered(organization, List.of(11L))).thenReturn(Map.of(11L, roofing));
        when(quotes.save(any())).thenAnswer(call -> {
            Quote quote = call.getArgument(0);
            quote.setId(42L);
            return quote;
        });
        service = new QuoteSubmissionService(quotes, clients, offerings, organizations, locations,
                new QuoteMapper(), events);
    }

    @Test
    void staffCreationPersistsTheLeadWithoutPublishingMailEvents() {
        var response = service.createForStaff("tcs", request);
        assertThat(response.id()).isEqualTo(42L);
        verify(quotes).save(any(Quote.class));
        verifyNoInteractions(events);
    }

    @Test
    void publicSubmissionStillPublishesTheSavedResponseAndOrganization() {
        var response = service.submit("tcs", request);
        var captured = org.mockito.ArgumentCaptor.forClass(QuoteSubmittedEvent.class);
        verify(events).publishEvent(captured.capture());
        assertThat(captured.getValue().quote()).isSameAs(response);
        assertThat(captured.getValue().organization()).isSameAs(organization);
    }
}
