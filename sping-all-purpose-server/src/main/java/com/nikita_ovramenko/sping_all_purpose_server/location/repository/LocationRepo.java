package com.nikita_ovramenko.sping_all_purpose_server.location.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;

@Repository
public interface LocationRepo extends JpaRepository<Location, Long> {

    /** Mirrors uq_location_client_address so an existing address is reused, not duplicated. */
    Optional<Location> findByClientAndCountryAndProvinceStateAndCityAndStreetAndPostalCode(
            Client client, Country country, String provinceState, String city, String street, String postalCode);

    List<Location> findByClientId(Long clientId);
}
