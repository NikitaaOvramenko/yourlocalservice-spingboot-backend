package com.nikita_ovramenko.sping_all_purpose_server.location.service;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.location.mapper.LocationMapper;
import com.nikita_ovramenko.sping_all_purpose_server.location.repository.LocationRepo;

@Service
public class LocationService {
    private final LocationRepo locationRepo;
    private final LocationMapper locationMapper;

    public LocationService(LocationRepo locationRepo, LocationMapper locationMapper) {
        this.locationRepo = locationRepo;
        this.locationMapper = locationMapper;
    }

}
