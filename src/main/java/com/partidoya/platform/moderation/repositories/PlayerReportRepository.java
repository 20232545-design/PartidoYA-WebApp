package com.partidoya.platform.moderation.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.users.models.UserId;

import java.util.List;
import java.util.Optional;

public interface PlayerReportRepository {
    Optional<PlayerReport> findById(Long id);
    boolean existsByMatchIdAndReporterIdAndReportedPlayerId(MatchId matchId, UserId reporterId, UserId reportedPlayerId);
    List<UserId> findReportedPlayerIdsByMatchIdAndReporterId(MatchId matchId, UserId reporterId);

    /** Reportes más recientes primero; {@code status} nulo significa todos los estados. */
    List<PlayerReport> findByStatus(PlayerReportStatus status, int page, int size);
    long countByStatus(PlayerReportStatus status);

    PlayerReport save(PlayerReport report);
}
