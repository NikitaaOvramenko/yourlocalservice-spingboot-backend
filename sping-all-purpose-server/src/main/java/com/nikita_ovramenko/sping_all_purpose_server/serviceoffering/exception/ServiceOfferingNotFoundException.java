package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

public class ServiceOfferingNotFoundException extends ResourceNotFoundException {

    public ServiceOfferingNotFoundException(Long id) {
        super("No service with id " + id);
    }
}
