package com.partidoya.platform.subscriptions.dto;

import java.time.LocalDateTime;

public record SubscriptionResource(
        Long id,
        String planCode,
        String status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        LocalDateTime nextRenewalAt,
        LocalDateTime canceledAt) {
}
