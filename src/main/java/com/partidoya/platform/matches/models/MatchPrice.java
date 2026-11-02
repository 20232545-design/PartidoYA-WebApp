package com.partidoya.platform.matches.models;

import java.math.BigDecimal;

public record MatchPrice(BigDecimal value) {
    public MatchPrice {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
    }
}
