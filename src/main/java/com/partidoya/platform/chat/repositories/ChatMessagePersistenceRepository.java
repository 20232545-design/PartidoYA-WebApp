package com.partidoya.platform.chat.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessagePersistenceRepository extends JpaRepository<ChatMessagePersistenceEntity, Long> {
    List<ChatMessagePersistenceEntity> findByMatchIdOrderByIdDesc(Long matchId, Pageable pageable);

    List<ChatMessagePersistenceEntity> findByMatchIdAndIdLessThanOrderByIdDesc(Long matchId, Long beforeId,
                                                                               Pageable pageable);
}
