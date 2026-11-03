package com.partidoya.platform.subscriptions.dto;

import java.util.List;

public record MySubscriptionResource(
        String currentPlanCode,
        boolean legacyAccess,
        SubscriptionResource subscription,
        List<SubscriptionPaymentResource> payments) {
}
