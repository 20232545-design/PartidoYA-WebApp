package com.partidoya.platform.realtime;

import com.partidoya.platform.users.models.UserId;

import java.security.Principal;
import java.util.Objects;

/**
 * Usuario de una sesión WebSocket. {@link #getName()} devuelve el id porque Spring lo usa
 * para resolver los destinos {@code /user/...} (por ejemplo, {@code convertAndSendToUser("42", ...)}).
 */
public record StompUser(UserId userId) implements Principal {
    public StompUser {
        Objects.requireNonNull(userId, "userId must not be null");
    }

    @Override
    public String getName() {
        return userId.value().toString();
    }
}
