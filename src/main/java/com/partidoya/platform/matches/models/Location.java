package com.partidoya.platform.matches.models;

public record Location(String value) {
    public Location {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("location must not be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("location must not exceed 200 characters");
        }
    }
}
