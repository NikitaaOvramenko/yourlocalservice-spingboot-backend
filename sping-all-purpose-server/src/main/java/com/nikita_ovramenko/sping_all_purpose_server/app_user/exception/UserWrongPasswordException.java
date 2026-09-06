package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

public class UserWrongPasswordException extends RuntimeException {
    public UserWrongPasswordException(String message) {
        super(message);
    }
}