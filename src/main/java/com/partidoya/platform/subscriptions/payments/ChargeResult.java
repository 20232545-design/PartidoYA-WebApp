package com.partidoya.platform.subscriptions.payments;

import java.time.LocalDateTime;

public record ChargeResult(
        String chargeId,
        int amountCents,
        String currency,
        String state,
        String responseCode,
        LocalDateTime occurredAt,
        Long userId,
        String planCode,
        String idempotencyKey) {
}
