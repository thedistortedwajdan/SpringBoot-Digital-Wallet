package com.app.wallet.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Account is deactivated");
    }
}
