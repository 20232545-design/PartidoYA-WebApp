package com.partidoya.platform.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo: acciones por REST que llegan en vivo por STOMP sobre un WebSocket real,
 * incluidas las reglas de autenticación y autorización de las suscripciones.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("migration-test")
class MatchRealtimeIntegrationTest {
    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort private int port;
    @Autowired private MockMvc mockMvc;
    @Autowired private SimpleBrokerMessageHandler broker;
    @Autowired private SimpUserRegistry userRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<StompSession> openSessions = new ArrayList<>();
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void tearDown() {
        openSessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        stompClient.stop();
    }

    @Test
    void joiningAndChattingArePushedLiveToTheMembersOfTheMatch() throws Exception {
        var organizer = registerPlayer();
        var player = registerPlayer();
        var matchId = createMatch(organizer.token());
        var chatTopic = "/topic/matches/" + matchId + "/chat";

        var organizerSession = connect(organizer.token());
        var organizerNotifications = subscribe(organizerSession, "/user/queue/notifications");
        var organizerChat = subscribe(organizerSession, chatTopic);
        awaitBrokerSubscription("/queue/notifications-user" + serverSessionId(organizer.id()));
        awaitBrokerSubscription(chatTopic);

        mockMvc.perform(post("/api/v1/matches/{id}/join", matchId)
                        .header("Authorization", bearer(player.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(player.id())))
                .andExpect(status().isOk());

        var notification = organizerNotifications.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(notification).as("organizer notification").isNotNull();
        assertThat(notification).containsEntry("type", "PLAYER_JOINED").containsEntry("read", false);
        assertThat(((Number) notification.get("matchId")).longValue()).isEqualTo(matchId);

        var playerSession = connect(player.token());
        var playerChat = subscribe(playerSession, chatTopic);
        awaitSubscriberCount(chatTopic, 2);

        mockMvc.perform(post("/api/v1/matches/{id}/messages", matchId)
                        .header("Authorization", bearer(player.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  ¿Quién lleva la pelota?  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("¿Quién lleva la pelota?"))
                .andExpect(jsonPath("$.authorId").value(player.id()));

        for (var chat : List.of(organizerChat, playerChat)) {
            var message = chat.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(message).as("chat broadcast").isNotNull();
            assertThat(message).containsEntry("content", "¿Quién lleva la pelota?")
                    .containsEntry("authorName", "Jugador Realtime");
        }

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(organizer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.content[0].type").value("PLAYER_JOINED"));
        mockMvc.perform(get("/api/v1/matches/{id}/messages", matchId)
                        .header("Authorization", bearer(organizer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void chatMessagesAreGroupedIntoOneLiveNotificationForMembersNotViewingTheChat() throws Exception {
        var organizer = registerPlayer();
        var player = registerPlayer();
        var matchId = createMatch(organizer.token());
        join(matchId, player);

        var organizerSession = connect(organizer.token());
        var organizerNotifications = subscribe(organizerSession, "/user/queue/notifications");
        awaitBrokerSubscription("/queue/notifications-user" + serverSessionId(organizer.id()));

        postMessage(matchId, player, "¿A qué hora llegan?");
        postMessage(matchId, player, "Yo llevo la pelota");

        var first = organizerNotifications.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        var second = organizerNotifications.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(first).as("first chat notification").isNotNull()
                .containsEntry("type", "CHAT_MESSAGES")
                .containsEntry("message", "Jugador Realtime escribió en el chat de «Pichanga en vivo».");
        assertThat(second).as("grouped chat notification").isNotNull()
                .containsEntry("id", first.get("id"))
                .containsEntry("groupedCount", 2)
                .containsEntry("message", "2 mensajes nuevos en el chat de «Pichanga en vivo».");

        // PLAYER_JOINED + el aviso agrupado del chat.
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(organizer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.content[0].type").value("CHAT_MESSAGES"));

        mockMvc.perform(post("/api/v1/notifications/matches/{id}/chat/read", matchId)
                        .header("Authorization", bearer(organizer.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(organizer.token())))
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Con el chat abierto ya no llegan avisos: los mensajes se ven en vivo.
        subscribe(organizerSession, "/topic/matches/" + matchId + "/chat");
        awaitBrokerSubscription("/topic/matches/" + matchId + "/chat");
        awaitViewing(organizer.id(), "/topic/matches/" + matchId + "/chat");
        postMessage(matchId, player, "¿Siguen ahí?");
        assertThat(organizerNotifications.poll(1, TimeUnit.SECONDS)).as("no notification while viewing").isNull();
    }

    @Test
    void outsiderCannotReadPostOrSubscribeToTheChat() throws Exception {
        var organizer = registerPlayer();
        var outsider = registerPlayer();
        var matchId = createMatch(organizer.token());

        mockMvc.perform(get("/api/v1/matches/{id}/messages", matchId).header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/matches/{id}/messages", matchId)
                        .header("Authorization", bearer(outsider.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hola\"}"))
                .andExpect(status().isForbidden());

        var errors = new ErrorRecordingHandler();
        var session = connect(outsider.token(), errors);
        session.subscribe("/topic/matches/" + matchId + "/chat", new QueueingFrameHandler(new LinkedBlockingQueue<>()));

        assertThat(errors.firstError().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("Subscription denied");
    }

    @Test
    void unknownDestinationsAreRejected() throws Exception {
        var user = registerPlayer();
        var errors = new ErrorRecordingHandler();
        var session = connect(user.token(), errors);

        session.subscribe("/topic/anything-else", new QueueingFrameHandler(new LinkedBlockingQueue<>()));

        assertThat(errors.firstError().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("Unknown destination");
    }

    @Test
    void connectionWithoutAValidTokenIsRefused() throws Exception {
        var errors = new ErrorRecordingHandler();
        var connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer not-a-real-token");

        var connection = stompClient.connectAsync(wsUrl(), new WebSocketHttpHeaders(), connectHeaders, errors);
        connection.whenComplete((session, failure) -> {
            if (failure != null) errors.firstError().complete(failure.getMessage());
        });

        assertThat(errors.firstError().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("Invalid or missing access token");
        assertThat(connection.isDone() && !connection.isCompletedExceptionally()).isFalse();
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    private StompSession connect(String token) throws Exception {
        return connect(token, new ErrorRecordingHandler());
    }

    private StompSession connect(String token, StompSessionHandlerAdapter handler) throws Exception {
        var connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", bearer(token));
        var session = stompClient.connectAsync(wsUrl(), new WebSocketHttpHeaders(), connectHeaders, handler)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        openSessions.add(session);
        return session;
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        var queue = new LinkedBlockingQueue<Map<String, Object>>();
        session.subscribe(destination, new QueueingFrameHandler(queue));
        return queue;
    }

    /** El SUBSCRIBE es asíncrono y el broker simple no confirma con RECEIPT: esperamos a verlo registrado. */
    private void awaitBrokerSubscription(String brokerDestination) throws InterruptedException {
        awaitSubscriberCount(brokerDestination, 1);
    }

    private void awaitSubscriberCount(String brokerDestination, int expected) throws InterruptedException {
        var deadline = Instant.now().plus(Duration.ofSeconds(TIMEOUT_SECONDS));
        while (subscriberCount(brokerDestination) < expected) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Subscription to " + brokerDestination + " was never registered");
            }
            Thread.sleep(20);
        }
    }

    private int subscriberCount(String brokerDestination) {
        var accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setDestination(brokerDestination);
        var probe = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return broker.getSubscriptionRegistry().findSubscriptions(probe).size();
    }

    private void join(long matchId, RegisteredUser player) throws Exception {
        mockMvc.perform(post("/api/v1/matches/{id}/join", matchId)
                        .header("Authorization", bearer(player.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(player.id())))
                .andExpect(status().isOk());
    }

    private void postMessage(long matchId, RegisteredUser author, String content) throws Exception {
        mockMvc.perform(post("/api/v1/matches/{id}/messages", matchId)
                        .header("Authorization", bearer(author.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated());
    }

    /** La presencia se basa en el registro de usuarios, que se actualiza aparte del broker. */
    private void awaitViewing(Long userId, String destination) throws InterruptedException {
        var deadline = Instant.now().plus(Duration.ofSeconds(TIMEOUT_SECONDS));
        while (userRegistry.findSubscriptions(subscription -> destination.equals(subscription.getDestination())
                && subscription.getSession().getUser().getName().equals(userId.toString())).isEmpty()) {
            if (Instant.now().isAfter(deadline)) throw new AssertionError("Chat presence was never registered");
            Thread.sleep(20);
        }
    }

    private String serverSessionId(Long userId) throws InterruptedException {
        var deadline = Instant.now().plus(Duration.ofSeconds(TIMEOUT_SECONDS));
        while (userRegistry.getUser(userId.toString()) == null) {
            if (Instant.now().isAfter(deadline)) throw new AssertionError("User session was never registered");
            Thread.sleep(20);
        }
        return userRegistry.getUser(userId.toString()).getSessions().iterator().next().getId();
    }

    private RegisteredUser registerPlayer() throws Exception {
        var email = "realtime-" + UUID.randomUUID() + "@test.com";
        var body = """
                {"email":"%s","firstName":"Jugador","lastName":"Realtime","password":"Segura#1234",
                "role":"JUGADOR","planCode":"JUGADOR_BASICO","phone":"+51987654321","district":"Miraflores",
                "primarySport":"Fútbol","skillLevel":"INTERMEDIATE","availability":["EVENING"],
                "termsAccepted":true,"privacyAccepted":true,"marketingConsent":false}
                """.formatted(email);
        var registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        var id = objectMapper.readTree(registration.getResponse().getContentAsString()).path("id").asLong();
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Segura#1234\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        var token = objectMapper.readTree(login.getResponse().getContentAsString()).path("token").asText();
        return new RegisteredUser(id, token);
    }

    private long createMatch(String organizerToken) throws Exception {
        var body = """
                {"sport":"Fútbol","title":"Pichanga en vivo","description":"Prueba de tiempo real",
                "address":"Av. Javier Prado 123, Lima","matchDate":"2030-01-15T19:00:00","totalSlots":10,
                "price":null,"latitude":null,"longitude":null,"requiresPlayerPayment":false}
                """;
        var result = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String wsUrl() {
        return "ws://localhost:" + port + RealtimeDestinations.ENDPOINT;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegisteredUser(Long id, String token) {
    }

    private record QueueingFrameHandler(BlockingQueue<Map<String, Object>> queue) implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.add((Map<String, Object>) payload);
        }
    }

    /** Registra el primer ERROR del servidor (frame ERROR, excepción o caída del transporte). */
    private static final class ErrorRecordingHandler extends StompSessionHandlerAdapter {
        private final CompletableFuture<String> firstError = new CompletableFuture<>();

        CompletableFuture<String> firstError() {
            return firstError;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            firstError.complete(String.valueOf(headers.getFirst("message")));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                    byte[] payload, Throwable exception) {
            firstError.complete(String.valueOf(exception.getMessage()));
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            firstError.complete(String.valueOf(exception.getMessage()));
        }
    }
}
