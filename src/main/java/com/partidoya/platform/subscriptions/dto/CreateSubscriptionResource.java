package com.partidoya.platform.subscriptions.dto;

public record CreateSubscriptionResource(
        String planCode,
        String tokenId,
        boolean termsAccepted,
        String idempotencyKey) {
}
