package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.matches.repositories.PlayerJoinRequestPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerJoinRequestPersistenceRepository extends JpaRepository<PlayerJoinRequestPersistenceEntity, Long> {
    Optional<PlayerJoinRequestPersistenceEntity> findFirstByMatchIdAndPlayerIdOrderBySubmittedAtDesc(Long matchId, Long playerId);
    boolean existsByMatchIdAndPlayerIdAndStatusIn(Long matchId, Long playerId, Collection<String> statuses);
    List<PlayerJoinRequestPersistenceEntity> findByMatchIdOrderBySubmittedAtDesc(Long matchId);
    List<PlayerJoinRequestPersistenceEntity> findByPlayerIdOrderBySubmittedAtDesc(Long playerId);
}
