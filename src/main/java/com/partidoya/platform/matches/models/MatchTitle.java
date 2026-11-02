package com.partidoya.platform.matches.models;

public record MatchTitle(String value) {
    public MatchTitle {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("title must not be null or blank");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("title must not exceed 100 characters");
        }
    }
}
