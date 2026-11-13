package com.partidoya.platform.notifications.models;

public enum NotificationType {
    PLAYER_JOINED,
    PLAYER_LEFT,
    MATCH_FULL,
    MATCH_CANCELLED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED,
    /** Agrupa los mensajes de chat de un partido en una sola notificación. */
    CHAT_MESSAGES
}
