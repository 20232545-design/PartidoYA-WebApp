package com.partidoya.platform.subscriptions.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Profile("!test")
public class CulqiChargeGateway implements OneTimePaymentGateway {
    private static final String CHARGES_API = "https://api.culqi.com/v2/charges";
    private final String secretKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CulqiChargeGateway(@Value("${culqi.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        requireConfigured();
        var body = new LinkedHashMap<String, Object>();
        body.put("amount", request.amountCents());
        body.put("currency_code", request.currency());
        body.put("email", request.email());
        body.put("source_id", request.tokenId());
        body.put("capture", true);
        body.put("description", request.description());
        body.put("metadata", Map.of(
                "user_id", request.userId().toString(),
                "plan_code", request.planCode(),
                "idempotency_key", request.idempotencyKey()));
        try {
            var httpRequest = HttpRequest.newBuilder(URI.create(CHARGES_API))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Charge-Idempotency-Key", request.idempotencyKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ChargeRejectedException(culqiMessage(response.body()));
            }
            // Si el banco exige 3D Secure, Culqi responde 200 con action_code = REVIEW y no crea el cargo.
            if ("REVIEW".equalsIgnoreCase(objectMapper.readTree(response.body()).path("action_code").asText(""))) {
                throw new ChargeRejectedException(
                        "Tu banco pidió verificación 3D Secure, que aún no está disponible. Prueba con otra tarjeta.");
            }
            var result = parse(response.body());
            // Culqi puede responder 200 con un cargo denegado (p. ej. outcome.type = operacion_denegada).
            if (!"Exitosa".equalsIgnoreCase(result.state())) {
                throw new ChargeRejectedException(culqiMessage(response.body()));
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La operación de pago fue interrumpida");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("El servicio de pagos de Culqi no está disponible");
        }
    }

    @Override
    public ChargeResult findCharge(String chargeId) {
        requireConfigured();
        if (chargeId == null || !chargeId.matches("chr_[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("El identificador del cargo no es válido");
        }
        try {
            var request = HttpRequest.newBuilder(URI.create(CHARGES_API + "/" + chargeId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("No se pudo verificar el cargo en Culqi");
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La verificación del pago fue interrumpida");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("El servicio de pagos de Culqi no está disponible");
        }
    }

    private ChargeResult parse(String responseBody) {
        try {
            var json = objectMapper.readTree(responseBody);
            if (!"charge".equals(json.path("object").asText())) {
                throw new IllegalStateException("Culqi devolvió una respuesta de cargo inválida");
            }
            var occurredAt = json.path("creation_date").asLong(json.path("created_at").asLong(0));
            var metadata = json.path("metadata");
            // La API v2 de Culqi informa el resultado en outcome.type / outcome.code, no en state / response_code.
            var outcome = json.path("outcome");
            var outcomeType = firstText(outcome, "type");
            var approved = "venta_exitosa".equalsIgnoreCase(outcomeType)
                    || "AUT0000".equalsIgnoreCase(firstText(outcome, "code"));
            var state = firstText(json, "state");
            if (state == null) state = approved ? "Exitosa" : outcomeType;
            var responseCode = firstText(json, "response_code");
            if (responseCode == null) responseCode = approved ? "venta_exitosa" : outcomeType;
            if (state == null || responseCode == null) {
                throw new IllegalStateException("Falta el resultado del cargo en la respuesta de Culqi");
            }
            return new ChargeResult(
                    requiredText(json, "id"),
                    json.path("amount").asInt(-1),
                    firstText(json, "currency_code", "currency"),
                    state,
                    responseCode,
                    occurredAt > 0
                            ? LocalDateTime.ofInstant(Instant.ofEpochSecond(occurredAt > 10_000_000_000L ? occurredAt / 1000 : occurredAt), ZoneOffset.UTC)
                            : LocalDateTime.now(ZoneOffset.UTC),
                    metadata.path("user_id").canConvertToLong() ? metadata.path("user_id").asLong() : parseLong(metadata.path("user_id").asText()),
                    firstText(metadata, "plan_code"),
                    firstText(metadata, "idempotency_key"));
        } catch (ChargeRejectedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("El cargo fue recibido por Culqi y está pendiente de conciliación", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Culqi devolvió una respuesta de cargo inválida");
        }
    }

    private String culqiMessage(String responseBody) {
        try {
            var json = objectMapper.readTree(responseBody);
            var outcome = json.path("outcome");
            if ("authentication_required".equalsIgnoreCase(firstText(outcome, "decline_code"))) {
                return "Tu banco pidió verificación 3D Secure, que aún no está disponible. Prueba con otra tarjeta.";
            }
            var userMessage = firstText(json, "user_message", "merchant_message");
            if (userMessage == null) userMessage = firstText(outcome, "user_message");
            return userMessage == null ? "Culqi rechazó el pago" : userMessage;
        } catch (Exception ignored) {
            return "Culqi rechazó el pago";
        }
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Culqi no está configurado");
        }
    }

    private static String requiredText(JsonNode node, String field) {
        var value = firstText(node, field);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta " + field + " en la respuesta de Culqi");
        return value;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (var field : fields) {
            if (node.hasNonNull(field) && !node.path(field).asText().isBlank()) return node.path(field).asText();
        }
        return null;
    }

    private static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
