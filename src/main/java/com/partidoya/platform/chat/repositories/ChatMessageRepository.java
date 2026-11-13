package com.partidoya.platform.chat.repositories;

import com.partidoya.platform.chat.models.ChatMessage;
import com.partidoya.platform.matches.models.MatchId;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);

    /**
     * Los {@code limit} mensajes más recientes del partido, del más nuevo al más antiguo.
     *
     * @param beforeId si no es nulo, solo mensajes con id menor (paginación hacia atrás)
     */
    List<ChatMessage> findLatestByMatchId(MatchId matchId, Long beforeId, int limit);
}
