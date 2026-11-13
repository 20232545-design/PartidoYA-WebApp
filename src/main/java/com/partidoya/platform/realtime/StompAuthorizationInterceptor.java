package com.partidoya.platform.realtime;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El WebSocket solo sirve para que el servidor empuje datos. Los clientes únicamente pueden suscribirse
 * a destinos autorizados; los mensajes se envían por REST, que ya valida, persiste y maneja errores.
 */
@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private final List<SubscriptionAuthorizer> authorizers;

    public StompAuthorizationInterceptor(List<SubscriptionAuthorizer> authorizers) {
        this.authorizers = List.copyOf(authorizers);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case SUBSCRIBE -> authorizeSubscription(message, accessor);
            case SEND -> throw new MessageDeliveryException("Sending over WebSocket is not supported; use the REST API");
            default -> {
                // CONNECT, DISCONNECT, UNSUBSCRIBE, ACK... no requieren autorización adicional.
            }
        }
        return message;
    }

    private void authorizeSubscription(Message<?> message, StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof StompUser user)) {
            throw new MessageDeliveryException("Authentication is required");
        }
        var destination = accessor.getDestination();
        if (destination == null) {
            throw new MessageDeliveryException("Destination is required");
        }
        var authorizer = authorizers.stream()
                .filter(candidate -> candidate.supports(destination))
                .findFirst()
                .orElseThrow(() -> new MessageDeliveryException("Unknown destination"));
        try {
            authorizer.authorize(user.userId(), destination);
        } catch (RuntimeException denied) {
            // Mismo error STOMP para "no existe" y "no autorizado": no revelamos qué partidos existen.
            throw new MessageDeliveryException(message, "Subscription denied", denied);
        }
    }
}
