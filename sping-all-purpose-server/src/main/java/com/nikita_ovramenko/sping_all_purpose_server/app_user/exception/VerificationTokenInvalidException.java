package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

/** The token does not exist -- mistyped, already used, or never issued. */
public class VerificationTokenInvalidException extends RuntimeException {
    public VerificationTokenInvalidException(String message) {
        super(message);
    }
}
