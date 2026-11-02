package com.partidoya.platform.matches.models;

import java.time.LocalDateTime;
import java.util.Objects;

public record MatchDate(LocalDateTime value) {
    public MatchDate {
        Objects.requireNonNull(value, "matchDate must not be null");
    }
}
