package com.partidoya.platform.subscriptions.payments;

public interface SubscriptionPaymentGateway {
    ProviderSubscription createSubscription(SubscriptionCreationRequest request);
    void cancelSubscription(String providerSubscriptionId);
    VerifiedSubscriptionEvent verifyEvent(String eventId);
}
