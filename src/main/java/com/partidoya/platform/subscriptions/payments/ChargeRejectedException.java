package com.partidoya.platform.subscriptions.payments;

public class ChargeRejectedException extends RuntimeException {
    public ChargeRejectedException(String message) {
        super(message);
    }
}
