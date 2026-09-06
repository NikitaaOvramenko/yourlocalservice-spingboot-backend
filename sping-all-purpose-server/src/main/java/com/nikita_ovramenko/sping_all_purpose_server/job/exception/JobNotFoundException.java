package com.nikita_ovramenko.sping_all_purpose_server.job.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

public class JobNotFoundException extends ResourceNotFoundException {

    public JobNotFoundException(Long id) {
        super("No job with id " + id);
    }
}
