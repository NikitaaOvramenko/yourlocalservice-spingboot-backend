package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.service.ClientResolver;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.BadRequestException;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.job.mapper.JobMapper;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;
import com.nikita_ovramenko.sping_all_purpose_server.job.repository.JobRepo;
import com.nikita_ovramenko.sping_all_purpose_server.job.service.JobService;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository.JobLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.service.LocationResolver;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationLookup;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

class JobServiceTest {
    private final JobRepo jobs = mock(JobRepo.class);
    private final QuoteRepo quotes = mock(QuoteRepo.class);
    private final ClientResolver clients = mock(ClientResolver.class);
    private final LocationResolver locations = mock(LocationResolver.class);
    private final OrganizationLookup organizations = mock(OrganizationLookup.class);
    private final OfferedServiceResolver offerings = mock(OfferedServiceResolver.class);
    private final JobService service = new JobService(jobs, mock(JobLineItemRepo.class), quotes,
            organizations, clients, locations, offerings, new JobMapper());
    private final Quote quote = new Quote();
    private final ServiceOffering roofing = new ServiceOffering();

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setSlug("tcs");
        quote.setId(42L);
        quote.setClient(new Client());
        quote.setLocation(new Location());
        quote.setOrganization(org);
        quote.setDescription("Repair roof");
        roofing.setId(11L);
        roofing.setName("Roofing");
        roofing.setSlug("roofing");
        QuoteLineItem line = new QuoteLineItem();
        line.setService(roofing);
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("125.50"));
        line.setDescription("North side");
        quote.addItem(line);
        when(quotes.findById(42L)).thenReturn(Optional.of(quote));
        when(jobs.save(any(Job.class))).thenAnswer(call -> {
            Job job = call.getArgument(0);
            job.setId(17L);
            return job;
        });
    }

    @Test
    void conversionCopiesAssociationsAndIndependentPricedLines() {
        Instant scheduled = Instant.parse("2026-10-01T10:00:00Z");
        var response = service.create(new JobCreateRequest(42L, null, null, null, null,
                null, scheduled, null));
        var captor = org.mockito.ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getClient()).isSameAs(quote.getClient());
        assertThat(saved.getOrganization()).isSameAs(quote.getOrganization());
        assertThat(saved.getLocation()).isSameAs(quote.getLocation());
        assertThat(response.quoteId()).isEqualTo(42L);
        assertThat(response.description()).isEqualTo("Repair roof");
        assertThat(response.scheduledAt()).isEqualTo(scheduled);
        assertThat(response.status()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(response.total()).isEqualByComparingTo("251.00");
        assertThat(saved.getItems()).singleElement().satisfies(line -> {
            assertThat(line.getJob()).isSameAs(saved);
            assertThat(line.getService()).isSameAs(roofing);
            assertThat(line.getDescription()).isEqualTo("North side");
            assertThat(line.getStatus()).isEqualTo(JobServiceStatus.PENDING);
            line.setUnitPrice(BigDecimal.ONE);
        });
        assertThat(quote.getItems().get(0).getUnitPrice()).isEqualByComparingTo("125.50");
        verifyNoInteractions(clients, locations, organizations, offerings);
    }

    @Test
    void aSecondJobForTheQuoteIsAConflictNamingTheExistingJob() {
        Job existing = new Job();
        existing.setId(17L);
        when(jobs.findByQuoteId(42L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.create(new JobCreateRequest(42L, null, null, null,
                null, null, null, null))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Quote 42 already has job 17");
        verify(jobs, never()).save(any());
    }

    @Test
    void walkInUsesSharedResolversAndAllowsRepeatedServices() {
        ClientRequest client = new ClientRequest("Jane", "Doe", "jane@example.com", null);
        LocationRequest location = new LocationRequest(Country.CANADA, "Ontario", "Toronto", "1 Main", "M1M1M1");
        when(organizations.requireBySlug("tcs")).thenReturn(quote.getOrganization());
        when(clients.upsert(client)).thenReturn(quote.getClient());
        when(locations.resolve(quote.getClient(), location)).thenReturn(quote.getLocation());
        when(offerings.requireAllOffered(quote.getOrganization(), List.of(11L, 11L)))
                .thenReturn(Map.of(11L, roofing));
        var line = new JobLineItemCreateRequest(11L, 1, BigDecimal.TEN, "Repair", null);
        var response = service.create(new JobCreateRequest(null, "tcs", client, location,
                List.of(line, line), "Walk-in", null, null));
        assertThat(response.quoteId()).isNull();
        assertThat(response.services()).hasSize(2);
        assertThat(response.total()).isEqualByComparingTo("20");
        verifyNoInteractions(quotes);
    }

    @Test
    void incompleteWalkInAndMixedCreationModesAreBadRequests() {
        assertThatThrownBy(() -> service.create(new JobCreateRequest(null, null, null, null,
                null, null, null, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(new JobCreateRequest(42L, "tcs", null, null,
                null, null, null, null))).isInstanceOf(BadRequestException.class);
        verify(jobs, never()).save(any());
    }
}
