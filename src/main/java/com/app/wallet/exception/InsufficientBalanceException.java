package com.app.wallet.exception;

public class InsufficientBalanceException extends BadRequestException {

    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}
