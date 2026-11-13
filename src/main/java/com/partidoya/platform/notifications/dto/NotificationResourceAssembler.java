package com.partidoya.platform.notifications.dto;

import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationPage;

public final class NotificationResourceAssembler {
    private NotificationResourceAssembler() {
    }

    public static NotificationResource toResource(Notification notification) {
        return new NotificationResource(
                notification.getId(),
                notification.getMatchId().value(),
                notification.getType().name(),
                notification.getMessage(),
                notification.isRead(),
                notification.getOccurredAt().toString(),
                notification.getGroupedCount());
    }

    public static NotificationPageResource toPageResource(NotificationPage page) {
        return new NotificationPageResource(
                page.content().stream().map(NotificationResourceAssembler::toResource).toList(),
                page.unreadCount(),
                page.page(),
                page.size());
    }
}
