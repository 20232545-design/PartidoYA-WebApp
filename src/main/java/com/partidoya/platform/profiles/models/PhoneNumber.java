package com.partidoya.platform.profiles.models;

import java.util.regex.Pattern;

public record PhoneNumber(String value) {
    private static final Pattern PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");

    public PhoneNumber {
        if (value != null && !value.isBlank() && !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("phone number format is invalid");
        }
    }
}
