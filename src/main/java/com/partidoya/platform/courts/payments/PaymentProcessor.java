package com.partidoya.platform.courts.payments;

public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}
