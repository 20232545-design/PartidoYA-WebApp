package com.partidoya.platform.notifications.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationType;
import com.partidoya.platform.users.models.UserId;

public final class NotificationPersistenceAssembler {
    private NotificationPersistenceAssembler() {
    }

    public static Notification toDomainFromPersistence(NotificationPersistenceEntity entity) {
        return Notification.restore(
                entity.getId(),
                new UserId(entity.getRecipientId()),
                new MatchId(entity.getMatchId()),
                NotificationType.valueOf(entity.getType()),
                entity.getMessage(),
                entity.getOccurredAt(),
                entity.getReadAt(),
                entity.getGroupedCount());
    }

    public static NotificationPersistenceEntity toPersistenceFromDomain(Notification notification) {
        var entity = new NotificationPersistenceEntity();
        if (notification.getId() != null) entity.setId(notification.getId());
        entity.setRecipientId(notification.getRecipientId().value());
        entity.setMatchId(notification.getMatchId().value());
        entity.setType(notification.getType().name());
        entity.setMessage(notification.getMessage());
        entity.setOccurredAt(notification.getOccurredAt());
        entity.setReadAt(notification.getReadAt());
        entity.setGroupedCount(notification.getGroupedCount());
        return entity;
    }
}
