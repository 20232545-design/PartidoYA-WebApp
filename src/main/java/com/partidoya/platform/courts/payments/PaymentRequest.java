package com.partidoya.platform.courts.payments;

import java.math.BigDecimal;

public record PaymentRequest(
        Long reservationId,
        BigDecimal amount,
        String currency,
        String sourceId,
        String email,
        String idempotencyKey,
        String description
) {
}
