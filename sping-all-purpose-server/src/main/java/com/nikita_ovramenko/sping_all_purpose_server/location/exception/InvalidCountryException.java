package com.nikita_ovramenko.sping_all_purpose_server.location.exception;

import java.util.Arrays;

import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;

/**
 * The legacy endpoint sent a country string that is not a known Country.
 *
 * <p>A dedicated type rather than a bare IllegalArgumentException so the exception
 * handler can map it to 400 without also swallowing genuine internal failures.
 * Previously Country.valueOf threw straight out of the service and produced a 500.
 */
public class InvalidCountryException extends RuntimeException {

    public InvalidCountryException(String value) {
        super("Unknown country '" + value + "'. Expected one of " + Arrays.toString(Country.values()));
    }
}
