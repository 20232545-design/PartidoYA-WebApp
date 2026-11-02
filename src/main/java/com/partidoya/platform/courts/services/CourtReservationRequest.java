package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;

import java.time.LocalDate;
import java.time.LocalTime;

public record CourtReservationRequest(
        CourtId courtId,
        UserId userId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String paymentMethod,
        String culqiToken,
        String payerEmail,
        String idempotencyKey
) {
}
