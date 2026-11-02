package com.partidoya.platform.payments.controllers;

import com.partidoya.platform.payments.dto.PaymentConfigResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/payments", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Payments", description = "Public payment configuration")
public class PaymentConfigController {
    private final String culqiPublicKey;

    public PaymentConfigController(@Value("${culqi.public-key:}") String culqiPublicKey) {
        this.culqiPublicKey = culqiPublicKey;
    }

    @GetMapping("/config")
    public ResponseEntity<PaymentConfigResource> getConfig() {
        return ResponseEntity.ok(new PaymentConfigResource(culqiPublicKey));
    }
}
