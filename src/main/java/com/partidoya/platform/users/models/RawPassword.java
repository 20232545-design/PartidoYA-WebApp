package com.partidoya.platform.users.models;

public record RawPassword(String value) {
    public RawPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password must not be null or blank");
        }
        if (value.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
    }
}
