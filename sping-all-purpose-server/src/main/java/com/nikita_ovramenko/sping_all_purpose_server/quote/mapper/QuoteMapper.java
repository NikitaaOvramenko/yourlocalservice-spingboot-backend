package com.nikita_ovramenko.sping_all_purpose_server.quote.mapper;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.interfaces.Mapper;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

@Component
public class QuoteMapper implements Mapper<Quote, QuoteDto> {

    @Override
    public QuoteDto toDto(Quote q) {

        Client client = q.getClient();
        Location location = q.getLocation();
        QuoteDto quoteDto = new QuoteDto(client.getName(), client.getLastname(), client.getLastname(),
                client.getPhone(), q.getWorkType(), q.getServiceType(), location.getCountry().toString(),
                location.getTown(), location.getStreet(), location.getPostalCode(), q.getDescription(),
                q.getPictures());

        return quoteDto;

    }

    @Override
    public Quote toEntity(QuoteDto d) {

        Quote q = new Quote();
        q.setDescription(d.description());
        q.setServiceType(d.service());
        q.setWorkType(d.workType());
        q.setPictures(d.images());

        return q;
    }

}
