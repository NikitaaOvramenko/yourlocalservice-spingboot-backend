package com.nikita_ovramenko.sping_all_purpose_server.quote.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

public class QuoteNotFoundException extends ResourceNotFoundException {

    public QuoteNotFoundException(Long id) {
        super("No quote with id " + id);
    }
}
