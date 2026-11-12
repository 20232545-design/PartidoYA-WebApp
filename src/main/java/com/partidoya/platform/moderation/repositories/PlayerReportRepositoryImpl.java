package com.partidoya.platform.moderation.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.users.models.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerReportRepositoryImpl implements PlayerReportRepository {
    private final PlayerReportPersistenceRepository repository;

    public PlayerReportRepositoryImpl(PlayerReportPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PlayerReport> findById(Long id) {
        return repository.findById(id).map(PlayerReportPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByMatchIdAndReporterIdAndReportedPlayerId(MatchId matchId, UserId reporterId,
                                                                   UserId reportedPlayerId) {
        return repository.existsByMatchIdAndReporterIdAndReportedPlayerId(
                matchId.value(), reporterId.value(), reportedPlayerId.value());
    }

    @Override
    public List<UserId> findReportedPlayerIdsByMatchIdAndReporterId(MatchId matchId, UserId reporterId) {
        return repository.findReportedPlayerIds(matchId.value(), reporterId.value()).stream()
                .map(UserId::new)
                .toList();
    }

    @Override
    public List<PlayerReport> findByStatus(PlayerReportStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entities = status == null
                ? repository.findAllByOrderBySubmittedAtDesc(pageable)
                : repository.findByStatusOrderBySubmittedAtDesc(status.name(), pageable);
        return entities.stream().map(PlayerReportPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public long countByStatus(PlayerReportStatus status) {
        return status == null ? repository.count() : repository.countByStatus(status.name());
    }

    @Override
    public PlayerReport save(PlayerReport report) {
        var entity = PlayerReportPersistenceAssembler.toPersistenceFromDomain(report);
        return PlayerReportPersistenceAssembler.toDomainFromPersistence(repository.save(entity));
    }
}
