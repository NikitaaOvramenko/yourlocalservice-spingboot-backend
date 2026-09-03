package com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception;

import java.util.Collection;


/**
 * The services exist but this organization does not offer them. Rendered as 422:
 * the request is well-formed, the combination is not valid.
 */
public class ServiceNotOfferedException extends RuntimeException {

    public ServiceNotOfferedException(String orgSlug, Collection<?> serviceIds) {
        super("Organization '" + orgSlug + "' does not offer service(s): " + serviceIds);
    }
}
