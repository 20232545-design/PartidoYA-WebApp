package com.partidoya.platform.moderation.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerReportPersistenceRepository extends JpaRepository<PlayerReportPersistenceEntity, Long> {
    boolean existsByMatchIdAndReporterIdAndReportedPlayerId(Long matchId, Long reporterId, Long reportedPlayerId);

    @Query("select r.reportedPlayerId from PlayerReportPersistenceEntity r where r.matchId = :matchId and r.reporterId = :reporterId")
    List<Long> findReportedPlayerIds(Long matchId, Long reporterId);

    List<PlayerReportPersistenceEntity> findAllByOrderBySubmittedAtDesc(Pageable pageable);
    List<PlayerReportPersistenceEntity> findByStatusOrderBySubmittedAtDesc(String status, Pageable pageable);
    long countByStatus(String status);
}
