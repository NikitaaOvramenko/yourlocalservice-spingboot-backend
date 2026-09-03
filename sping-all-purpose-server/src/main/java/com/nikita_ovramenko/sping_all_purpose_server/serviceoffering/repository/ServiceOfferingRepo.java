package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

@Repository
public interface ServiceOfferingRepo extends JpaRepository<ServiceOffering, Long> {

    Optional<ServiceOffering> findBySlugIgnoreCase(String slug);

    Optional<ServiceOffering> findByNameIgnoreCase(String name);
}
