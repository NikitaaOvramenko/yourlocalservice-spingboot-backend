package com.nikita_ovramenko.sping_all_purpose_server.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;

public interface LocationRepo extends JpaRepository<Location, Long> {

}
