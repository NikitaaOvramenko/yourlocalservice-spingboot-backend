package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception;

import java.util.Collection;

/** The request named services that are not in the catalog at all. Rendered as 400. */
public class UnknownServiceException extends RuntimeException {

    public UnknownServiceException(Collection<?> unknown) {
        super("Unknown service(s): " + unknown);
    }
}
