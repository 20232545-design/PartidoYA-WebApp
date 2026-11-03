package com.partidoya.platform.subscriptions.payments;

public record ChargeRequest(
        Long userId,
        String email,
        String planCode,
        int amountCents,
        String currency,
        String tokenId,
        String idempotencyKey,
        String description) {
}
