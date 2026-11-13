package com.partidoya.platform.notifications.dto;

public record NotificationResource(
        Long id,
        Long matchId,
        String type,
        String message,
        boolean read,
        String occurredAt,
        int groupedCount) {
}
