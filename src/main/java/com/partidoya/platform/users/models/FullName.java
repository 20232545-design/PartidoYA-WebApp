package com.partidoya.platform.users.models;

public record FullName(String value) {
    public FullName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fullName must not be null or blank");
        }
        if (value.length() > 120) {
            throw new IllegalArgumentException("fullName must not exceed 120 characters");
        }
    }
}
