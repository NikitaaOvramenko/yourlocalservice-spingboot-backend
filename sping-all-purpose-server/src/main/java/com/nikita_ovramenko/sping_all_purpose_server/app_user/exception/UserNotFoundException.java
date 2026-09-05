package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
