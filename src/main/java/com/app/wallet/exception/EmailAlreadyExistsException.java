package com.app.wallet.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email [" + email + "] already exists.");
    }
    public EmailAlreadyExistsException(String email, Throwable cause) {
        super("Email [" + email + "] already exists, exception details [" + cause + "]");
    }

}
