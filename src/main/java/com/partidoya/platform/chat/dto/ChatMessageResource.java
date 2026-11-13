package com.partidoya.platform.chat.dto;

import com.partidoya.platform.chat.models.ChatMessageView;

public record ChatMessageResource(
        Long id,
        Long matchId,
        Long authorId,
        String authorName,
        String content,
        String sentAt) {

    public static ChatMessageResource from(ChatMessageView view) {
        var message = view.message();
        return new ChatMessageResource(
                message.getId(),
                message.getMatchId().value(),
                message.getAuthorId().value(),
                view.authorName(),
                message.getContent(),
                message.getSentAt().toString());
    }
}
