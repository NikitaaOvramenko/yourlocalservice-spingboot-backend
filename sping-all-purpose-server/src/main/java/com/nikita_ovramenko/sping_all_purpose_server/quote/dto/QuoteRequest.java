package com.nikita_ovramenko.sping_all_purpose_server.quote.dto;

import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemRequest;

public record QuoteRequest(
        @NotNull @Valid ClientRequest client,
        @NotNull @Valid LocationRequest location,
        @NotEmpty @Size(max = 20) List<@Valid QuoteLineItemRequest> services,
        @Size(max = 4000) String description,
        @Size(max = 20) List<@NotBlank @Size(max = 1024) String> pictureKeys) {
}
