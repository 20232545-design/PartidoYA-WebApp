package com.partidoya.platform.notifications.models;

import java.util.List;

public record NotificationPage(List<Notification> content, long unreadCount, int page, int size) {
    public NotificationPage {
        content = List.copyOf(content);
    }
}
