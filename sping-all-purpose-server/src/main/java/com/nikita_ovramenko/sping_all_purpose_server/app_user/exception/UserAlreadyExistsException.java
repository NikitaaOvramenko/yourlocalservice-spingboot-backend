package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
