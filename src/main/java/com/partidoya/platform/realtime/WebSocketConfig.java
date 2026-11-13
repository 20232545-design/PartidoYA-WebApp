package com.partidoya.platform.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * STOMP sobre WebSocket con el broker en memoria de Spring. Es suficiente mientras el backend corra
 * en una sola instancia; con varias habría que usar un broker externo (RabbitMQ, Redis) como relay.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    /** Latido servidor↔cliente: mantiene viva la conexión tras proxies y detecta clientes caídos. */
    private static final long HEARTBEAT_MS = 10_000;

    private final StompAuthenticationInterceptor authenticationInterceptor;
    private final StompAuthorizationInterceptor authorizationInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            StompAuthenticationInterceptor authenticationInterceptor,
            StompAuthorizationInterceptor authorizationInterceptor,
            @Value("${cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String origins,
            @Value("${cors.allowed-origin-patterns:https://*.onrender.com,https://*.partidoya.pro}") String patterns) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
        // Mismos orígenes que CORS: el handshake WebSocket no pasa por la configuración CORS de Spring Security.
        this.allowedOriginPatterns = Stream.of(origins, patterns)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(RealtimeDestinations.ENDPOINT).setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(RealtimeDestinations.TOPIC_PREFIX, RealtimeDestinations.QUEUE_PREFIX)
                .setHeartbeatValue(new long[]{HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(stompHeartbeatScheduler());
        registry.setUserDestinationPrefix(RealtimeDestinations.USER_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // El orden importa: primero se identifica al usuario y después se autoriza lo que pide.
        registration.interceptors(authenticationInterceptor, authorizationInterceptor);
    }

    @Bean
    public ThreadPoolTaskScheduler stompHeartbeatScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        return scheduler;
    }
}
