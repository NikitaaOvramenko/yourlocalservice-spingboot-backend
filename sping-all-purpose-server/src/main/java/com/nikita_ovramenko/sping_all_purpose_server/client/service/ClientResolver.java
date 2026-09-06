package com.nikita_ovramenko.sping_all_purpose_server.client.service;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientRequest;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;

/**
 * Finds a client by email or creates one, updating the stored details either way.
 *
 * <p>Extracted from QuoteSubmissionService so quotes and walk-in jobs cannot drift into
 * two different notions of client identity. client.email is unique, so it is the key.
 */
@Service
public class ClientResolver {

    private final ClientRepo clientRepo;

    public ClientResolver(ClientRepo clientRepo) {
        this.clientRepo = clientRepo;
    }

    public Client upsert(ClientRequest request) {
        Client client = clientRepo.findByEmailIgnoreCase(request.email()).orElseGet(Client::new);
        // NOTE: this overwrites an existing client's details on every submission, which
        // is the pre-existing behaviour. On the public funnel, which is unauthenticated,
        // it means anyone who knows an email address can rewrite that person's record.
        // Left as-is; it belongs with a wider auth review rather than here.
        client.setEmail(request.email());
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setPhone(request.phone());
        return clientRepo.save(client);
    }
}
