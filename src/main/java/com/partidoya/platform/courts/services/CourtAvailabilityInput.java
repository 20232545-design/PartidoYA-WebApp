package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;

import java.time.LocalDate;
import java.time.LocalTime;

public record CourtAvailabilityInput(
        CourtId courtId,
        LocalDate date,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        CourtAvailabilityType type,
        String reason
) {
}
