package com.partidoya.platform.subscriptions.payments;

import java.time.LocalDateTime;

public record VerifiedSubscriptionEvent(
        String eventId,
        String type,
        String subscriptionId,
        String transactionId,
        LocalDateTime occurredAt,
        String rawPayload) {
}
