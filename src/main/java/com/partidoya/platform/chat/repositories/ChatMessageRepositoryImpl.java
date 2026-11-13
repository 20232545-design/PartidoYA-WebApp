package com.partidoya.platform.chat.repositories;

import com.partidoya.platform.chat.models.ChatMessage;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final ChatMessagePersistenceRepository repository;

    public ChatMessageRepositoryImpl(ChatMessagePersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        var entity = new ChatMessagePersistenceEntity();
        if (message.getId() != null) entity.setId(message.getId());
        entity.setMatchId(message.getMatchId().value());
        entity.setAuthorId(message.getAuthorId().value());
        entity.setContent(message.getContent());
        entity.setSentAt(message.getSentAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public List<ChatMessage> findLatestByMatchId(MatchId matchId, Long beforeId, int limit) {
        var page = PageRequest.of(0, limit);
        var entities = beforeId == null
                ? repository.findByMatchIdOrderByIdDesc(matchId.value(), page)
                : repository.findByMatchIdAndIdLessThanOrderByIdDesc(matchId.value(), beforeId, page);
        return entities.stream().map(ChatMessageRepositoryImpl::toDomain).toList();
    }

    private static ChatMessage toDomain(ChatMessagePersistenceEntity entity) {
        return ChatMessage.restore(
                entity.getId(),
                new MatchId(entity.getMatchId()),
                new UserId(entity.getAuthorId()),
                entity.getContent(),
                entity.getSentAt());
    }
}
