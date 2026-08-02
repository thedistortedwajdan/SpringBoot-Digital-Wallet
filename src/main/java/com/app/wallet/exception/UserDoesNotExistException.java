package com.app.wallet.exception;

public class UserDoesNotExistException extends RuntimeException {
    public UserDoesNotExistException(String email) {

        super("User [" + email + "] does not exist.");
    }
}
