package com.partidoya.platform.notifications.realtime;

import com.partidoya.platform.realtime.RealtimeDestinations;
import com.partidoya.platform.realtime.SubscriptionAuthorizer;
import com.partidoya.platform.users.models.UserId;
import org.springframework.stereotype.Component;

/**
 * Cualquier usuario autenticado puede suscribirse a su propia cola: Spring resuelve
 * {@code /user/queue/notifications} contra la sesión, así que nadie recibe la cola de otro.
 */
@Component
public class NotificationSubscriptionAuthorizer implements SubscriptionAuthorizer {
    private static final String DESTINATION = RealtimeDestinations.USER_PREFIX + RealtimeDestinations.NOTIFICATIONS_QUEUE;

    @Override
    public boolean supports(String destination) {
        return DESTINATION.equals(destination);
    }

    @Override
    public void authorize(UserId userId, String destination) {
        // Sin restricciones adicionales: la sesión ya está autenticada.
    }
}
