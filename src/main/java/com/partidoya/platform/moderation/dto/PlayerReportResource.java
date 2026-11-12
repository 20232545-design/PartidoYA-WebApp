package com.partidoya.platform.moderation.dto;

public record PlayerReportResource(
        Long id,
        Long matchId,
        String matchTitle,
        Long reporterId,
        String reporterName,
        Long reportedPlayerId,
        String reportedPlayerName,
        String reason,
        String details,
        String status,
        String submittedAt,
        String reviewedAt,
        Long reviewedBy,
        String resolutionNote) {
}
