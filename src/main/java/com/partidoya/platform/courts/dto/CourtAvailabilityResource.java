package com.partidoya.platform.courts.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CourtAvailabilityResource(
        Long id,
        Long courtId,
        String courtName,
        LocalDate date,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        String type,
        String reason,
        LocalDateTime createdAt
) {
}
