package com.nikita_ovramenko.sping_all_purpose_server.client.service;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientDto;
import com.nikita_ovramenko.sping_all_purpose_server.client.mapper.ClientMapper;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;

import jakarta.transaction.Transactional;

@Service
public class ClientService {

    private final ClientRepo clientRepo;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepo clientRepo, ClientMapper clientMapper) {
        this.clientRepo = clientRepo;
        this.clientMapper = clientMapper;
    }

    @Transactional
    public ClientDto saveClient(ClientDto clientDto) {
        Client client = clientMapper.toEntity(clientDto);
        clientRepo.save(client);

        return clientMapper.toDto(client);
    }

    @Transactional
    public ClientDto updateClient(ClientDto clientDto) {
        Client client = clientMapper.toEntity(clientDto);
        clientRepo.save(client);

        return clientMapper.toDto(client);
    }

}
