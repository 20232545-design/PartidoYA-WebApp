package com.partidoya.platform.subscriptions.dto;

import java.time.LocalDateTime;

public record SubscriptionPaymentResource(
        Long id,
        String planCode,
        int amountCents,
        String currency,
        String status,
        LocalDateTime paidAt,
        String failureReason) {
}
