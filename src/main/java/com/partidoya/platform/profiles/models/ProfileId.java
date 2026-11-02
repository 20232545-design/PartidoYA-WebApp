package com.partidoya.platform.profiles.models;

public record ProfileId(Long value) {
    public ProfileId {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("profileId must be a positive value");
        }
    }
}
