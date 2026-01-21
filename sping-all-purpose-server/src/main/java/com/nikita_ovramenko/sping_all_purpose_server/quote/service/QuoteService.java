package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.mapper.ClientMapper;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;
import com.nikita_ovramenko.sping_all_purpose_server.email.EmailService;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.repository.LocationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.mapper.QuoteMapper;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;
import com.nikita_ovramenko.sping_all_purpose_server.quote.repository.QuoteRepo;

@Service
public class QuoteService {

    private final QuoteRepo quoteRepo;
    private final QuoteMapper quoteMapper;
    private final EmailService emailService;
    private final ClientRepo clientRepo;
    private final LocationRepo locationRepo;

    public QuoteService(QuoteRepo quoteRepo, QuoteMapper quoteMapper, ClientRepo clientRepo,
            LocationRepo locationRepo, EmailService emailService) {
        this.quoteRepo = quoteRepo;
        this.quoteMapper = quoteMapper;
        this.clientRepo = clientRepo;
        this.locationRepo = locationRepo;
        this.emailService = emailService;
    }

    public QuoteDto save(QuoteDto quoteDto) {

        Quote q = quoteMapper.toEntity(quoteDto);

        Client client = new Client();
        Location location = new Location();
        List<Location> locations = new ArrayList<>();

        Country country = Country.valueOf(quoteDto.country());

        location.setTown(quoteDto.town());
        location.setCountry(country);
        location.setStreet(quoteDto.street());
        location.setPostalCode(quoteDto.postal_code());
        locations.add(location);

        client.setEmail(quoteDto.email());
        client.setName(quoteDto.name());
        client.setLastname(quoteDto.lastname());
        client.setLocations(locations);
        client.setPhone(quoteDto.phone());

        q.setClient(client);
        q.setLocation(location);

        location.setClient(client);

        List<Quote> quotes = new ArrayList<>();
        quotes.add(q);
        client.setQuotes(quotes);

        clientRepo.save(client);
        locationRepo.save(location);
        Quote saved = quoteRepo.save(q);

        emailService.sendFormResponseToClient(client, location, q);

        return quoteMapper.toDto(saved);

    }

}
