package com.app.wallet.model;

import java.util.Optional;

public enum Role {
    USER,
    ADMIN;

    private static final String PREFIX = "ROLE_";

    public String authority() {
        return PREFIX + name();
    }

    public static Optional<Role> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
