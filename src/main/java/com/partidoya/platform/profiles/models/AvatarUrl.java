package com.partidoya.platform.profiles.models;

public record AvatarUrl(String value) {
    public AvatarUrl {
        if (value != null && value.length() > 500) {
            throw new IllegalArgumentException("avatarUrl must not exceed 500 characters");
        }
    }
}
