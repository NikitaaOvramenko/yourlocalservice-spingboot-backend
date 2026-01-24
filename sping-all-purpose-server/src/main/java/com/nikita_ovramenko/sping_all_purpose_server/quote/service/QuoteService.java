package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.nikita_ovramenko.sping_all_purpose_server.quote.status.QuoteStatus;

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

    @Transactional
    public QuoteDto save(QuoteDto quoteDto) {

        Quote q = quoteMapper.toEntity(quoteDto);
        q.setStatus(QuoteStatus.BEGAN);

        Client client = clientRepo.findByEmail(quoteDto.email())
                .orElseGet(Client::new);

        client.setEmail(quoteDto.email());
        client.setName(quoteDto.name());
        client.setLastname(quoteDto.lastname());
        client.setPhone(quoteDto.phone());

        if (client.getLocations() == null)
            client.setLocations(new ArrayList<>());
        if (client.getQuotes() == null)
            client.setQuotes(new ArrayList<>());

        Location location = new Location();
        location.setTown(quoteDto.town());
        location.setCountry(Country.valueOf(quoteDto.country()));
        location.setStreet(quoteDto.street());
        location.setPostalCode(quoteDto.postal_code());

        location.setClient(client);
        client.getLocations().add(location);

        q.setClient(client);
        q.setLocation(location);
        client.getQuotes().add(q);

        Client savedClient = clientRepo.save(client);

        emailService.sendFormResponseToClient(savedClient, location, q);

        System.out.println("I was here !");

        return quoteMapper.toDto(q);
    }

}
