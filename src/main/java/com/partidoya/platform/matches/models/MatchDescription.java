package com.partidoya.platform.matches.models;

public record MatchDescription(String value) {
    public MatchDescription {
        if (value != null && value.length() > 500) {
            throw new IllegalArgumentException("description must not exceed 500 characters");
        }
    }
}
