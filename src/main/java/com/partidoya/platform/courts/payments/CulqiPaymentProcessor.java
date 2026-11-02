package com.partidoya.platform.courts.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.partidoya.platform.courts.payments.PaymentProcessor;
import com.partidoya.platform.courts.payments.PaymentRequest;
import com.partidoya.platform.courts.payments.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Profile("!test")
public class CulqiPaymentProcessor implements PaymentProcessor {
    private static final URI CHARGES_URI = URI.create("https://api.culqi.com/v2/charges");
    private static final Set<String> SUCCESS_VALUES = Set.of(
            "exitosa",
            "venta_exitosa",
            "autorizada",
            "autorizado",
            "capturada",
            "capturado",
            "successful",
            "succeeded",
            "captured");

    private final String secretKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CulqiPaymentProcessor(@Value("${culqi.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public PaymentResult process(PaymentRequest request) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Culqi secret key is not configured");
        }
        if (request.sourceId() == null || request.sourceId().isBlank()) {
            throw new IllegalArgumentException("Culqi payment token is required");
        }
        try {
            var body = new HashMap<String, Object>();
            body.put("amount", toCents(request.amount()));
            body.put("currency_code", request.currency());
            body.put("email", request.email());
            body.put("source_id", request.sourceId());
            if (request.description() != null && !request.description().isBlank()) {
                body.put("description", request.description());
            }
            body.put("metadata", Map.of("reservation_id", request.reservationId().toString()));

            var builder = HttpRequest.newBuilder(CHARGES_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                builder.header("X-Charge-Idempotency-Key", request.idempotencyKey());
            }

            var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("No se pudo procesar el pago.");
            }
            JsonNode json = objectMapper.readTree(response.body());
            var responseCode = textAt(json, "response_code");
            var state = textAt(json, "state");
            var outcomeType = json.path("outcome").path("type").asText("");
            var outcomeCode = json.path("outcome").path("code").asText("");
            var status = firstPresent(state, responseCode, outcomeType, outcomeCode, "UNKNOWN");
            var approved = isApproved(state) || isApproved(responseCode) || isApproved(outcomeType)
                    || "AUT0000".equalsIgnoreCase(outcomeCode);
            return new PaymentResult(approved, json.path("id").asText(null), status, json.path("currency").asText(request.currency()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El pago fue interrumpido.");
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo procesar el pago.");
        }
    }

    private static int toCents(BigDecimal amount) {
        return amount.movePointRight(2).intValueExact();
    }

    private static boolean isApproved(String value) {
        return value != null && SUCCESS_VALUES.contains(value.trim().toLowerCase());
    }

    private static String textAt(JsonNode json, String field) {
        return json.hasNonNull(field) ? json.path(field).asText("") : "";
    }

    private static String firstPresent(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "UNKNOWN";
    }
}
