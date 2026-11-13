package com.partidoya.platform.notifications.models;

import java.util.Objects;

/**
 * Se publica al crear o actualizar (agrupar) una notificación; el envío en tiempo real
 * ocurre recién tras el commit. El cliente reemplaza la notificación por id.
 */
public record NotificationChanged(Notification notification) {
    public NotificationChanged {
        Objects.requireNonNull(notification, "notification must not be null");
        Objects.requireNonNull(notification.getId(), "notification must be persisted before being published");
    }
}
