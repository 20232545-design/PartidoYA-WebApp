package com.partidoya.platform.courts.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ManagedReservationView(
        Long id,
        Long userId,
        String customerName,
        Long courtId,
        String courtName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        String currency,
        String providerReference,
        String status,
        String paymentStatus,
        LocalDateTime createdAt
) {
}
