package com.partidoya.platform.courts.payments;

public record PaymentResult(boolean approved, String providerReference, String status, String currency) {
}
