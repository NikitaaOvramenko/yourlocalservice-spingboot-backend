package com.nikita_ovramenko.sping_all_purpose_server.organization.exception;

/** No active organization with the requested slug. Rendered as 404. */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(String slug) {
        super("No active organization with slug '" + slug + "'");
    }
}
