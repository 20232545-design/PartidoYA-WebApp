package com.partidoya.platform.courts.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReserveCourtResource(
        Long userId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String paymentMethod,
        String culqiToken,
        String payerEmail,
        String idempotencyKey
) {
}
