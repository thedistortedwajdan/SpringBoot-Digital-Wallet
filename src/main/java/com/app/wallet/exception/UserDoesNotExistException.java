package com.app.wallet.exception;

public class UserDoesNotExistException extends RuntimeException {
    public UserDoesNotExistException(String email) {

        super("User [" + email + "] does not exist.");
    }

    public UserDoesNotExistException(Long id) {
        super("User with id [" + id + "] does not exist.");
    }
}
