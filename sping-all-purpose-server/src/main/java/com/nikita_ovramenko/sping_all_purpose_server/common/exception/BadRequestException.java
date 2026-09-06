package com.nikita_ovramenko.sping_all_purpose_server.common.exception;

/**
 * The request is malformed in a way Bean Validation cannot express -- typically a
 * combination of fields that is individually valid but nonsensical together.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
