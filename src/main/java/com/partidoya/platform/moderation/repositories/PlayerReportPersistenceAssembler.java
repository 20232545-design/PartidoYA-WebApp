package com.partidoya.platform.moderation.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.moderation.models.ReportReason;
import com.partidoya.platform.users.models.UserId;

public final class PlayerReportPersistenceAssembler {
    private PlayerReportPersistenceAssembler() {
    }

    public static PlayerReport toDomainFromPersistence(PlayerReportPersistenceEntity entity) {
        return PlayerReport.restore(
                entity.getId(),
                new MatchId(entity.getMatchId()),
                new UserId(entity.getReporterId()),
                new UserId(entity.getReportedPlayerId()),
                ReportReason.valueOf(entity.getReason()),
                entity.getDetails(),
                PlayerReportStatus.valueOf(entity.getStatus()),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy() != null ? new UserId(entity.getReviewedBy()) : null,
                entity.getResolutionNote());
    }

    public static PlayerReportPersistenceEntity toPersistenceFromDomain(PlayerReport report) {
        var entity = new PlayerReportPersistenceEntity();
        if (report.getId() != null) entity.setId(report.getId());
        entity.setMatchId(report.getMatchId().value());
        entity.setReporterId(report.getReporterId().value());
        entity.setReportedPlayerId(report.getReportedPlayerId().value());
        entity.setReason(report.getReason().name());
        entity.setDetails(report.getDetails());
        entity.setStatus(report.getStatus().name());
        entity.setSubmittedAt(report.getSubmittedAt());
        entity.setReviewedAt(report.getReviewedAt());
        entity.setReviewedBy(report.getReviewedBy() != null ? report.getReviewedBy().value() : null);
        entity.setResolutionNote(report.getResolutionNote());
        return entity;
    }
}
