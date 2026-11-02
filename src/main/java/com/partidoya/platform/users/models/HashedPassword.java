package com.partidoya.platform.users.models;

public record HashedPassword(String value) {
    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("hashedPassword must not be null or blank");
        }
    }
}
