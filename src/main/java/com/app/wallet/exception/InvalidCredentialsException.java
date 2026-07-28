package com.app.wallet.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException()
    {
        super("invalid credentials");
    }
}
