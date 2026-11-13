package com.partidoya.platform.chat.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/** Mensaje del chat grupal de un partido. Es inmutable: no se edita ni se borra. */
@Getter
public class ChatMessage {
    public static final int MAX_CONTENT_LENGTH = 1000;

    private final Long id;
    private final MatchId matchId;
    private final UserId authorId;
    private final String content;
    private final LocalDateTime sentAt;

    private ChatMessage(Long id, MatchId matchId, UserId authorId, String content, LocalDateTime sentAt) {
        this.id = id;
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.authorId = Objects.requireNonNull(authorId, "authorId must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
    }

    /** Crea un mensaje nuevo con el contenido sin espacios sobrantes en los extremos. */
    public static ChatMessage post(MatchId matchId, UserId authorId, String content, LocalDateTime now) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        var trimmed = content.strip();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content must not exceed %d characters".formatted(MAX_CONTENT_LENGTH));
        }
        return new ChatMessage(null, matchId, authorId, trimmed, now);
    }

    /** Reconstruye un mensaje ya persistido, sin volver a aplicar las reglas de creación. */
    public static ChatMessage restore(Long id, MatchId matchId, UserId authorId, String content, LocalDateTime sentAt) {
        return new ChatMessage(Objects.requireNonNull(id, "id must not be null"), matchId, authorId, content, sentAt);
    }
}
