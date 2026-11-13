package com.partidoya.platform.notifications.realtime;

import com.partidoya.platform.notifications.dto.NotificationResourceAssembler;
import com.partidoya.platform.notifications.models.NotificationChanged;
import com.partidoya.platform.realtime.RealtimeDestinations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Empuja cada notificación al usuario conectado. Corre tras el commit para no anunciar datos
 * que luego se revierten; si el usuario no está conectado, la verá al cargar la lista por REST.
 */
@Component
public class NotificationPusher {
    private static final Logger log = LoggerFactory.getLogger(NotificationPusher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationPusher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onNotificationChanged(NotificationChanged event) {
        var notification = event.notification();
        try {
            messagingTemplate.convertAndSendToUser(
                    notification.getRecipientId().value().toString(),
                    RealtimeDestinations.NOTIFICATIONS_QUEUE,
                    NotificationResourceAssembler.toResource(notification));
        } catch (MessagingException pushFailure) {
            // La notificación ya está guardada: un fallo de entrega en vivo no debe afectar la operación.
            log.warn("Could not push notification {} in real time", notification.getId(), pushFailure);
        }
    }
}
