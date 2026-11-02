package com.partidoya.platform.courts.payments;

import com.partidoya.platform.courts.payments.PaymentProcessor;
import com.partidoya.platform.courts.payments.PaymentRequest;
import com.partidoya.platform.courts.payments.PaymentResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class SimulatedPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(PaymentRequest request) {
        return new PaymentResult(true, "SIM-%d".formatted(request.reservationId()), "APPROVED", request.currency());
    }
}
