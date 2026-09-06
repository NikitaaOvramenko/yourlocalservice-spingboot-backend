package com.nikita_ovramenko.sping_all_purpose_server.common.exception;

/**
 * The request is valid but conflicts with the current state -- a second job for a quote
 * that already has one, demoting the last remaining admin, adding a service twice to the
 * same quote.
 *
 * <p>Thrown deliberately rather than letting the database raise a constraint violation:
 * DataIntegrityViolationException is also a 409, but its message is necessarily generic
 * ("The request conflicts with existing data") and cannot say which rule was broken.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
