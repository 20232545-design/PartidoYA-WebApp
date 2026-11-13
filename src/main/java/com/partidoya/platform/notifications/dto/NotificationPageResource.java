package com.partidoya.platform.notifications.dto;

import java.util.List;

public record NotificationPageResource(
        List<NotificationResource> content,
        long unreadCount,
        int page,
        int size) {
}
