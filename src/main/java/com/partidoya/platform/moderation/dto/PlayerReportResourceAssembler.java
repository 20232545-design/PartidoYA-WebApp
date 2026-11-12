package com.partidoya.platform.moderation.dto;

import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportPage;
import com.partidoya.platform.moderation.models.PlayerReportView;

import java.time.LocalDateTime;

public final class PlayerReportResourceAssembler {
    private PlayerReportResourceAssembler() {
    }

    public static SubmittedPlayerReportResource toSubmittedResource(PlayerReport report) {
        return new SubmittedPlayerReportResource(
                report.getId(),
                report.getMatchId().value(),
                report.getReportedPlayerId().value(),
                report.getStatus().name(),
                format(report.getSubmittedAt()));
    }

    public static PlayerReportResource toResource(PlayerReportView view) {
        var report = view.report();
        return new PlayerReportResource(
                report.getId(),
                report.getMatchId().value(),
                view.matchTitle(),
                report.getReporterId().value(),
                view.reporterName(),
                report.getReportedPlayerId().value(),
                view.reportedPlayerName(),
                report.getReason().name(),
                report.getDetails(),
                report.getStatus().name(),
                format(report.getSubmittedAt()),
                format(report.getReviewedAt()),
                report.getReviewedBy() != null ? report.getReviewedBy().value() : null,
                report.getResolutionNote());
    }

    public static PlayerReportPageResource toPageResource(PlayerReportPage page) {
        return new PlayerReportPageResource(
                page.content().stream().map(PlayerReportResourceAssembler::toResource).toList(),
                page.totalElements(),
                page.page(),
                page.size());
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
}
