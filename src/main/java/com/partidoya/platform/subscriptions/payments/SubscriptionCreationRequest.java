package com.partidoya.platform.subscriptions.payments;

public record SubscriptionCreationRequest(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        String district,
        String planCode,
        String tokenId,
        String idempotencyKey) {
}
