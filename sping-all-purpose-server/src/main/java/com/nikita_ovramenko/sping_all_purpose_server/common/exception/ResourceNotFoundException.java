package com.nikita_ovramenko.sping_all_purpose_server.common.exception;

/**
 * Base for "the thing you addressed does not exist" failures, mapped once to 404.
 *
 * <p>Per-entity subclasses live in their own entity's exception package, matching the
 * existing OrganizationNotFoundException layout, so a caller can still catch a specific
 * type -- but GlobalExceptionHandler only needs one handler for all of them.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
