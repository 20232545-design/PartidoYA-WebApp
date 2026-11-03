package com.partidoya.platform.subscriptions.payments;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestSubscriptionGateway implements SubscriptionPaymentGateway {
    @Override
    public ProviderSubscription createSubscription(SubscriptionCreationRequest request) {
        return new ProviderSubscription("customer-test", "card-test", "subscription-" + request.userId());
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
    }

    @Override
    public VerifiedSubscriptionEvent verifyEvent(String eventId) {
        throw new IllegalArgumentException("Tests must provide a verified event explicitly");
    }
}
