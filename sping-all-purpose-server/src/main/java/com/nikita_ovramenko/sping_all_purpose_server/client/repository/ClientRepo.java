package com.nikita_ovramenko.sping_all_purpose_server.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;

@Repository
public interface ClientRepo extends JpaRepository<Client, Long> {

}
