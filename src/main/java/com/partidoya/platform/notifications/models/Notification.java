package com.partidoya.platform.notifications.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aviso dirigido a un usuario sobre algo que ocurrió en un partido en el que participa.
 * Las de chat son agrupadas: una sola por partido que acumula los mensajes no leídos.
 */
@Getter
public class Notification {
    public static final int MAX_MESSAGE_LENGTH = 500;

    private final Long id;
    private final UserId recipientId;
    private final MatchId matchId;
    private final NotificationType type;
    private String message;
    private LocalDateTime occurredAt;
    private LocalDateTime readAt;
    /** Cantidad de eventos agrupados (mensajes de chat sin leer); 1 para las notificaciones simples. */
    private int groupedCount;

    private Notification(Long id, UserId recipientId, MatchId matchId, NotificationType type, String message,
                         LocalDateTime occurredAt, LocalDateTime readAt, int groupedCount) {
        this.id = id;
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.readAt = readAt;
        if (groupedCount < 1) {
            throw new IllegalArgumentException("groupedCount must be at least 1");
        }
        this.groupedCount = groupedCount;
    }

    /** Crea una notificación nueva, aún no leída. */
    public static Notification create(UserId recipientId, MatchId matchId, NotificationType type, String message,
                                      LocalDateTime now) {
        return new Notification(null, recipientId, matchId, type, validMessage(message), now, null, 1);
    }

    /** Primera notificación de chat de un partido para el destinatario. */
    public static Notification chatActivity(UserId recipientId, MatchId matchId, String matchTitle, String authorName,
                                            LocalDateTime now) {
        return create(recipientId, matchId, NotificationType.CHAT_MESSAGES,
                chatMessageText(1, matchTitle, authorName), now);
    }

    /** Reconstruye una notificación ya persistida, sin volver a aplicar las reglas de creación. */
    public static Notification restore(Long id, UserId recipientId, MatchId matchId, NotificationType type,
                                       String message, LocalDateTime occurredAt, LocalDateTime readAt,
                                       int groupedCount) {
        return new Notification(Objects.requireNonNull(id, "id must not be null"), recipientId, matchId, type,
                message, occurredAt, readAt, groupedCount);
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** Idempotente: si ya estaba leída conserva la fecha de la primera lectura. */
    public void markAsRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = Objects.requireNonNull(now, "now must not be null");
        }
    }

    /**
     * Suma un mensaje de chat a esta notificación. Si ya se había leído, vuelve a quedar sin leer
     * y el conteo empieza de nuevo: solo cuenta lo que el usuario todavía no vio.
     */
    public void registerChatMessage(String matchTitle, String authorName, LocalDateTime now) {
        if (type != NotificationType.CHAT_MESSAGES) {
            throw new IllegalStateException("Only chat notifications can group messages");
        }
        groupedCount = isRead() ? 1 : groupedCount + 1;
        readAt = null;
        occurredAt = Objects.requireNonNull(now, "now must not be null");
        message = validMessage(chatMessageText(groupedCount, matchTitle, authorName));
    }

    static String chatMessageText(int count, String matchTitle, String authorName) {
        var title = "«" + matchTitle + "»";
        return count == 1
                ? "%s escribió en el chat de %s.".formatted(authorName, title)
                : "%d mensajes nuevos en el chat de %s.".formatted(count, title);
    }

    private static String validMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        var trimmed = message.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message must not exceed %d characters".formatted(MAX_MESSAGE_LENGTH));
        }
        return trimmed;
    }
}
