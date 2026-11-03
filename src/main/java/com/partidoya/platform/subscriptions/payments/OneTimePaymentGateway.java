package com.partidoya.platform.subscriptions.payments;

public interface OneTimePaymentGateway {
    ChargeResult charge(ChargeRequest request);
    ChargeResult findCharge(String chargeId);
}
