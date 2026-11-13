package com.partidoya.platform.notifications.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationType;
import com.partidoya.platform.users.models.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    /** Solo devuelve la notificación si pertenece al destinatario indicado. */
    Optional<Notification> findByIdAndRecipientId(Long id, UserId recipientId);

    /** La notificación más reciente de ese tipo para el usuario en el partido (para agrupar). */
    Optional<Notification> findLatest(UserId recipientId, MatchId matchId, NotificationType type);

    /** Ordenadas por el último evento ocurrido, más recientes primero. */
    List<Notification> findByRecipientId(UserId recipientId, int page, int size);

    long countUnreadByRecipientId(UserId recipientId);

    /** @return cantidad de notificaciones que pasaron a leídas */
    int markAllAsRead(UserId recipientId, LocalDateTime readAt);

    /** @return cantidad de notificaciones del tipo indicado que pasaron a leídas en ese partido */
    int markAsReadByMatchAndType(UserId recipientId, MatchId matchId, NotificationType type, LocalDateTime readAt);
}
