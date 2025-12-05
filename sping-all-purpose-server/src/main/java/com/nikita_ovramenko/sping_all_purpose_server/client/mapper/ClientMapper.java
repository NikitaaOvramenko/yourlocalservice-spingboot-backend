package com.nikita_ovramenko.sping_all_purpose_server.client.mapper;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientDto;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.interfaces.Mapper;

public class ClientMapper implements Mapper<Client, ClientDto> {
    public ClientMapper() {
    }

    @Override
    public ClientDto toDto(Client client) {
        ClientDto dto = new ClientDto(client.getName(), client.getLastname(), client.getEmail());
        return dto;
    }

    @Override
    public Client toEntity(ClientDto dto) {
        Client client = new Client();

        client.setEmail(dto.email());
        client.setName(dto.name());
        client.setLastname(dto.lastname());

        return client;
    }

}
