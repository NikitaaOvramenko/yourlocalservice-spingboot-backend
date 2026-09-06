package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.quote.exception.QuoteNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.exception.QuoteLineItemNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.repository.QuoteLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.service.QuoteLineItemService;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/**
 * Plain unit test -- no Spring context, no database.
 *
 * <p>The important cases here are the ones where a nested URL could otherwise be
 * decoration: /quotes/1/items/{id} must not be able to reach a line belonging to a
 * different quote.
 */
class QuoteLineItemServiceTest {

    private QuoteRepo quoteRepo;
    private QuoteLineItemRepo lineItemRepo;
    private OfferedServiceResolver offeredServiceResolver;
    private QuoteLineItemService service;

    private Organization organization;
    private ServiceOffering roofing;
    private Quote quote1;
    private Quote quote7;
    private QuoteLineItem lineOnQuote7;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(3L);
        organization.setSlug("tcs");

        roofing = new ServiceOffering();
        roofing.setId(11L);
        roofing.setName("Roof repair");
        roofing.setSlug("roof-repair");

        quote1 = new Quote();
        quote1.setId(1L);
        quote1.setOrganization(organization);

        quote7 = new Quote();
        quote7.setId(7L);
        quote7.setOrganization(organization);

        lineOnQuote7 = new QuoteLineItem();
        lineOnQuote7.setId(42L);
        lineOnQuote7.setQuote(quote7);
        lineOnQuote7.setService(roofing);
        lineOnQuote7.setQuantity(1);

        quoteRepo = mock(QuoteRepo.class);
        lineItemRepo = mock(QuoteLineItemRepo.class);
        offeredServiceResolver = mock(OfferedServiceResolver.class);

        given(quoteRepo.findById(1L)).willReturn(Optional.of(quote1));
        given(quoteRepo.findById(7L)).willReturn(Optional.of(quote7));
        given(lineItemRepo.findById(42L)).willReturn(Optional.of(lineOnQuote7));
        given(lineItemRepo.save(any(QuoteLineItem.class))).willAnswer(call -> call.getArgument(0));
        given(offeredServiceResolver.requireOffered(organization, 11L)).willReturn(roofing);

        service = new QuoteLineItemService(quoteRepo, lineItemRepo, offeredServiceResolver);
    }

    /**
     * The line belongs to quote 7. Addressing it under quote 1 must be a 404, not an
     * edit -- otherwise the nested path constrains nothing.
     */
    @Test
    void aLineBelongingToAnotherQuoteCannotBeUpdated() {
        assertThatThrownBy(() -> service.update(1L, 42L,
                new QuoteLineItemUpdateRequest(new BigDecimal("1.00"), null, null)))
                .isInstanceOf(QuoteLineItemNotFoundException.class)
                .hasMessageContaining("on quote 1");

        assertThat(lineOnQuote7.getUnitPrice()).isNull();
        verify(lineItemRepo, never()).save(any(QuoteLineItem.class));
    }

    @Test
    void aLineBelongingToAnotherQuoteCannotBeDeleted() {
        assertThatThrownBy(() -> service.delete(1L, 42L))
                .isInstanceOf(QuoteLineItemNotFoundException.class);

        verify(lineItemRepo, never()).delete(any(QuoteLineItem.class));
    }

    @Test
    void aLineOnTheAddressedQuoteIsUpdated() {
        QuoteLineItemResponse updated = service.update(7L, 42L,
                new QuoteLineItemUpdateRequest(new BigDecimal("1250.50"), 2, "roof"));

        assertThat(updated.unitPrice()).isEqualByComparingTo("1250.50");
        assertThat(updated.quantity()).isEqualTo(2);
        assertThat(updated.lineTotal()).isEqualByComparingTo("2501.00");
    }

    /** Null means "leave unchanged", applied uniformly across the admin API. */
    @Test
    void nullFieldsLeaveTheExistingValuesAlone() {
        lineOnQuote7.setUnitPrice(new BigDecimal("100.00"));
        lineOnQuote7.setDescription("original");

        service.update(7L, 42L, new QuoteLineItemUpdateRequest(null, 5, null));

        assertThat(lineOnQuote7.getUnitPrice()).isEqualByComparingTo("100.00");
        assertThat(lineOnQuote7.getDescription()).isEqualTo("original");
        assertThat(lineOnQuote7.getQuantity()).isEqualTo(5);
    }

    /** uq_quote_service forbids it; caught here so the message can name the service. */
    @Test
    void addingAServiceAlreadyOnTheQuoteIsRejected() {
        given(lineItemRepo.existsByQuoteIdAndServiceId(1L, 11L)).willReturn(true);

        assertThatThrownBy(() -> service.add(1L,
                new QuoteLineItemCreateRequest(11L, 1, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Roof repair");

        verify(lineItemRepo, never()).save(any(QuoteLineItem.class));
    }

    @Test
    void aServiceTheOrganizationDoesNotOfferIsRejected() {
        given(offeredServiceResolver.requireOffered(organization, 99L))
                .willThrow(new ServiceNotOfferedException("tcs", java.util.Set.of(99L)));

        assertThatThrownBy(() -> service.add(1L,
                new QuoteLineItemCreateRequest(99L, 1, null, null)))
                .isInstanceOf(ServiceNotOfferedException.class);

        verify(lineItemRepo, never()).save(any(QuoteLineItem.class));
    }

    @Test
    void addingToAnUnknownQuoteIsNotFound() {
        given(quoteRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(999L,
                new QuoteLineItemCreateRequest(11L, 1, null, null)))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void aNewLineCarriesItsPriceAndQuantity() {
        given(lineItemRepo.existsByQuoteIdAndServiceId(1L, 11L)).willReturn(false);

        QuoteLineItemResponse added = service.add(1L,
                new QuoteLineItemCreateRequest(11L, 3, new BigDecimal("20.00"), "gutters"));

        assertThat(added.serviceId()).isEqualTo(11L);
        assertThat(added.quantity()).isEqualTo(3);
        assertThat(added.lineTotal()).isEqualByComparingTo("60.00");
    }
}
