package com.partidoya.platform.profiles.models;

public record SportName(String value) {
    public SportName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sportName must not be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("sportName must not exceed 50 characters");
        }
    }
}
