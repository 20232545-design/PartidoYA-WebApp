package com.partidoya.platform.subscriptions.payments;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("test")
public class TestChargeGateway implements OneTimePaymentGateway {
    @Override
    public ChargeResult charge(ChargeRequest request) {
        return new ChargeResult("chr_test_" + request.userId(), request.amountCents(), request.currency(),
                "Exitosa", "venta_exitosa", LocalDateTime.now(), request.userId(), request.planCode(), request.idempotencyKey());
    }

    @Override
    public ChargeResult findCharge(String chargeId) {
        throw new IllegalArgumentException("Las pruebas deben proporcionar el cargo conciliado");
    }
}
