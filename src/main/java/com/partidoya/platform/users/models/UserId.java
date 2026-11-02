package com.partidoya.platform.users.models;

public record UserId(Long value) {
    public UserId {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("userId must be a positive value");
        }
    }
}
