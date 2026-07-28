package com.app.wallet.exception;

public class UserDoesNotExistException extends Exception {
    public UserDoesNotExistException(String email) {

        super("Email [" + email + "] does not exist.");
    }
}
