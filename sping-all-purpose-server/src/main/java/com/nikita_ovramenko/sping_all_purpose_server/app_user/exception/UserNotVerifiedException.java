package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

public class UserNotVerifiedException extends RuntimeException {
    public UserNotVerifiedException(String message) {
        super(message);
    }
}
