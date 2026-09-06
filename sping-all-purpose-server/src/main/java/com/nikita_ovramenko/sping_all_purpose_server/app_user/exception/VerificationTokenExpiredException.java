package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

/**
 * The token was real but is past its expiry. Distinct from invalid so the caller can
 * offer a resend rather than just saying "wrong link".
 */
public class VerificationTokenExpiredException extends RuntimeException {
    public VerificationTokenExpiredException(String message) {
        super(message);
    }
}
