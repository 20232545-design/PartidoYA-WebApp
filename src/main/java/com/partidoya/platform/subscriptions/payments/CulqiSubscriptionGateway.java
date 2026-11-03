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
public class CulqiSubscriptionGateway implements SubscriptionPaymentGateway {
    private static final String API = "https://api.culqi.com/v2";
    private final String secretKey;
    private final Map<String, String> planIds;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CulqiSubscriptionGateway(
            @Value("${culqi.secret-key:}") String secretKey,
            @Value("${culqi.subscription-plan-ids.jugador-plus:}") String playerPlus,
            @Value("${culqi.subscription-plan-ids.cancha-emprendedor:}") String courtEntrepreneur,
            @Value("${culqi.subscription-plan-ids.cancha-business:}") String courtBusiness) {
        this.secretKey = secretKey;
        this.planIds = Map.of(
                "JUGADOR_PLUS", playerPlus,
                "CANCHA_EMPRENDEDOR", courtEntrepreneur,
                "CANCHA_BUSINESS", courtBusiness);
    }

    @Override
    public ProviderSubscription createSubscription(SubscriptionCreationRequest request) {
        requireConfigured();
        var planId = planIds.get(request.planCode());
        if (planId == null || planId.isBlank()) {
            throw new IllegalStateException("Culqi plan is not configured for " + request.planCode());
        }
        var customerBody = new LinkedHashMap<String, Object>();
        customerBody.put("first_name", request.firstName());
        customerBody.put("last_name", request.lastName());
        customerBody.put("email", request.email());
        customerBody.put("address", fallback(request.district(), "Lima"));
        customerBody.put("address_city", fallback(request.district(), "Lima"));
        customerBody.put("country_code", "PE");
        customerBody.put("phone_number", fallback(request.phone(), "999999999"));
        customerBody.put("metadata", Map.of("user_id", request.userId().toString()));
        var customer = post("/customers", customerBody);
        var customerId = requiredText(customer, "id");

        var card = post("/cards", Map.of("customer_id", customerId, "token_id", request.tokenId()));
        var cardId = requiredText(card, "id");

        var subscriptionBody = new LinkedHashMap<String, Object>();
        subscriptionBody.put("card_id", cardId);
        subscriptionBody.put("plan_id", planId);
        subscriptionBody.put("tyc", true);
        subscriptionBody.put("metadata", Map.of(
                "user_id", request.userId().toString(),
                "plan_code", request.planCode(),
                "idempotency_key", request.idempotencyKey()));
        var subscription = post("/recurrent/subscriptions/create", subscriptionBody);
        return new ProviderSubscription(customerId, cardId, requiredText(subscription, "id"));
    }

    @Override
    public void cancelSubscription(String providerSubscriptionId) {
        requireConfigured();
        send(HttpRequest.newBuilder(URI.create(API + "/recurrent/subscriptions/" + providerSubscriptionId)).DELETE());
    }

    @Override
    public VerifiedSubscriptionEvent verifyEvent(String eventId) {
        requireConfigured();
        var response = send(HttpRequest.newBuilder(URI.create(API + "/events/" + eventId)).GET());
        try {
            var json = objectMapper.readTree(response);
            var verifiedId = requiredText(json, "id");
            if (!verifiedId.equals(eventId)) {
                throw new IllegalArgumentException("Webhook event identifier does not match Culqi");
            }
            var type = requiredText(json, "type");
            var object = json.path("data").path("object");
            var subscriptionId = firstText(object, "subscription_id", "subscription.id", "subscription");
            if ((subscriptionId == null || subscriptionId.isBlank()) && type.startsWith("subscription.")) {
                subscriptionId = text(object, "id");
            }
            var transactionId = firstText(object, "charge_id", "transaction_id", "id");
            var occurredAt = timestamp(json.path("creation_date").asLong(json.path("created_at").asLong(0)));
            return new VerifiedSubscriptionEvent(verifiedId, type, subscriptionId, transactionId, occurredAt, response);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Culqi returned an invalid event");
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(API + path))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            return objectMapper.readTree(send(builder));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Culqi request could not be created");
        }
    }

    private String send(HttpRequest.Builder builder) {
        try {
            var request = builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey).build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Culqi rejected the subscription operation");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Culqi subscription operation was interrupted");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Culqi subscription service is unavailable");
        }
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Culqi recurring payments are not configured");
        }
    }

    private static String requiredText(JsonNode node, String field) {
        var value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Culqi response does not contain " + field);
        }
        return value;
    }

    private static String firstText(JsonNode node, String... paths) {
        for (var path : paths) {
            var current = node;
            for (var segment : path.split("\\.")) {
                current = current.path(segment);
            }
            if (!current.isMissingNode() && !current.isNull()) {
                var value = current.asText();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static LocalDateTime timestamp(long value) {
        if (value <= 0) return LocalDateTime.now(ZoneOffset.UTC);
        var seconds = value > 10_000_000_000L ? value / 1000 : value;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
    }
}
