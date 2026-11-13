package com.partidoya.platform.realtime;

import com.partidoya.platform.auth.security.AccessTokenAuthenticator;
import com.partidoya.platform.users.models.UserId;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Autentica la sesión STOMP con el mismo JWT del API REST. Los navegadores no permiten cabeceras
 * en el handshake WebSocket, por eso el token viaja en la cabecera {@code Authorization} del frame CONNECT
 * (y no en la URL, donde terminaría en logs de proxies).
 */
@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {
    static final String AUTHORIZATION_HEADER = "Authorization";

    private final AccessTokenAuthenticator authenticator;

    public StompAuthenticationInterceptor(AccessTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        var user = authenticator.authenticate(accessor.getFirstNativeHeader(AUTHORIZATION_HEADER))
                .orElseThrow(() -> new MessageDeliveryException("Invalid or missing access token"));
        accessor.setUser(new StompUser(new UserId(user.userId())));
        return message;
    }
}
