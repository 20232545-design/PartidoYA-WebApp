package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.PlayerJoinRequest;
import com.partidoya.platform.matches.models.JoinRequestStatus;
import com.partidoya.platform.matches.models.MatchId;

import java.util.List;
import java.util.Optional;

public interface PlayerJoinRequestRepository {
    Optional<PlayerJoinRequest> findById(Long id);
    Optional<PlayerJoinRequest> findLatestByMatchIdAndPlayerId(MatchId matchId, UserId playerId);
    boolean existsByMatchIdAndPlayerIdAndStatusIn(MatchId matchId, UserId playerId, List<JoinRequestStatus> statuses);
    List<PlayerJoinRequest> findByMatchId(MatchId matchId);
    List<PlayerJoinRequest> findByPlayerId(UserId playerId);
    PlayerJoinRequest save(PlayerJoinRequest joinRequest);
}
