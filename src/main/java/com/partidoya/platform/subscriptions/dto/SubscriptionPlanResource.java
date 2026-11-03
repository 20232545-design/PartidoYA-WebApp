package com.partidoya.platform.subscriptions.dto;

public record SubscriptionPlanResource(
        String code,
        String displayName,
        String description,
        String role,
        int amountCents,
        String currency,
        String intervalUnit,
        boolean paid,
        boolean subscriptionAvailable) {
}
