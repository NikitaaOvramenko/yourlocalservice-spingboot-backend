package com.nikita_ovramenko.sping_all_purpose_server.quote.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.model.QuoteLineItem;

/**
 * Quote -> API response.
 *
 * <p>There is no toEntity counterpart. A valid Quote needs a resolved Organization,
 * Client and Location, none of which a request DTO carries, so building one is the
 * submission service's job -- the old symmetric Mapper interface forced a half-built
 * entity to be returned here and finished by hand elsewhere.
 */
@Component
public class QuoteMapper {

    public QuoteResponse toResponse(Quote quote) {
        Client client = quote.getClient();
        Location location = quote.getLocation();

        List<QuoteLineItemResponse> services = quote.getItems().stream()
                .map(QuoteMapper::toServiceResponse)
                .toList();

        return new QuoteResponse(
                quote.getId(),
                quote.getOrganization().getSlug(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getExpiresAt(),
                new ClientSummary(client.getId(), client.getFirstName(), client.getLastName(),
                        client.getEmail(), client.getPhone()),
                new LocationSummary(location.getId(), location.getCountry(), location.getProvinceState(),
                        location.getCity(), location.getStreet(), location.getPostalCode()),
                services,
                quote.getDescription(),
                List.copyOf(quote.getPictures()));
    }

    private static QuoteLineItemResponse toServiceResponse(QuoteLineItem item) {
        return new QuoteLineItemResponse(
                item.getId(),
                item.getService().getId(),
                item.getService().getName(),
                item.getService().getSlug(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.lineTotal(),
                item.getDescription());
    }
}
