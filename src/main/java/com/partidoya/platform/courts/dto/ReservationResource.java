package com.partidoya.platform.courts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationResource(
        Long id,
        Long userId,
        Long courtId,
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
