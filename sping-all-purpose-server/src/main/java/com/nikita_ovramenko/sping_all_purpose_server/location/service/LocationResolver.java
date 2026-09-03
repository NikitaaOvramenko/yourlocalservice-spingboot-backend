package com.nikita_ovramenko.sping_all_purpose_server.location.service;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationRequest;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.repository.LocationRepo;

/**
 * Finds an existing address for a client or creates it once.
 *
 * <p>Replaces the previous behaviour of constructing a new Location on every single
 * submission, which left repeat clients with a pile of duplicate address rows.
 */
@Service
public class LocationResolver {

    private final LocationRepo locationRepo;

    public LocationResolver(LocationRepo locationRepo) {
        this.locationRepo = locationRepo;
    }

    public Location resolve(Client client, LocationRequest request) {
        String provinceState = normalize(request.provinceState());
        String city = normalize(request.city());
        String street = normalize(request.street());
        String postalCode = normalizePostalCode(request.postalCode());

        return locationRepo
                .findByClientAndCountryAndProvinceStateAndCityAndStreetAndPostalCode(
                        client, request.country(), provinceState, city, street, postalCode)
                .orElseGet(() -> {
                    Location location = new Location();
                    location.setClient(client);
                    location.setCountry(request.country());
                    location.setProvinceState(provinceState);
                    location.setCity(city);
                    location.setStreet(street);
                    location.setPostalCode(postalCode);
                    return locationRepo.save(location);
                });
    }

    /** Trim, collapse internal whitespace, and never return null -- the unique key columns are NOT NULL. */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\s+", " ");
    }

    /**
     * Postal codes have to compare equal across formatting differences or dedup fails:
     * "a1a 1a1", "A1A-1A1" and "A1A1A1" are one address, and US ZIP+4 is written both
     * with and without the hyphen.
     */
    private static String normalizePostalCode(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase().replaceAll("[\s-]", "");
    }
}
