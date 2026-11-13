package com.partidoya.platform.chat.models;

import java.util.Objects;

/** Mensaje junto con el nombre del autor, que es lo que muestra la pantalla del chat. */
public record ChatMessageView(ChatMessage message, String authorName) {
    public ChatMessageView {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(authorName, "authorName must not be null");
    }
}
