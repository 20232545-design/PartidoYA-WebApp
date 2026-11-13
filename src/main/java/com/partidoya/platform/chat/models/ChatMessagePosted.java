package com.partidoya.platform.chat.models;

import java.util.Objects;

/** Se publica al guardar un mensaje; la difusión por WebSocket ocurre recién tras el commit. */
public record ChatMessagePosted(ChatMessageView view) {
    public ChatMessagePosted {
        Objects.requireNonNull(view, "view must not be null");
        Objects.requireNonNull(view.message().getId(), "message must be persisted before being published");
    }
}
