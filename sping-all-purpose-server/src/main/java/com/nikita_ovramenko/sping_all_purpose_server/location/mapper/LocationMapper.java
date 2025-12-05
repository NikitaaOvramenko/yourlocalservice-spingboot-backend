package com.nikita_ovramenko.sping_all_purpose_server.location.mapper;

import com.nikita_ovramenko.sping_all_purpose_server.interfaces.Mapper;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationDto;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;

public class LocationMapper implements Mapper<Location, LocationDto> {

    @Override
    public LocationDto toDto(Location e) {
        LocationDto locationDto = new LocationDto(e.getCountry(), e.getTown(), e.getStreet(), e.getPostalCode(),
                e.getId());
        return locationDto;
    }

    @Override
    public Location toEntity(LocationDto d) {
        Location location = new Location();

        location.setCountry(d.country());
        location.setId(d.clientId());
        location.setTown(d.town());
        location.setPostalCode(d.postalCode());
        location.setStreet(d.street());

        return location;

    }

}
