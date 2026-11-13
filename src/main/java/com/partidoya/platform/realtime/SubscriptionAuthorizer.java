package com.partidoya.platform.realtime;

import com.partidoya.platform.users.models.UserId;

/**
 * Cada módulo que expone un destino STOMP registra un autorizador para él.
 * Un destino que ningún autorizador reconoce se rechaza (denegado por defecto).
 */
public interface SubscriptionAuthorizer {
    boolean supports(String destination);

    /** Lanza una excepción si el usuario no puede suscribirse al destino. */
    void authorize(UserId userId, String destination);
}
