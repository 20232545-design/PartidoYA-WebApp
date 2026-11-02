package com.partidoya.platform.courts.models;

public record CourtId(Long value) {
    public CourtId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("court id must be positive");
        }
    }
}
