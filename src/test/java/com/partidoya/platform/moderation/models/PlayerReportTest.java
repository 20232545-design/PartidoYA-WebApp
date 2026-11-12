package com.partidoya.platform.moderation.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerReportTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 18, 0);
    private static final UserId REPORTER = new UserId(7L);
    private static final UserId REPORTED = new UserId(18L);
    private static final UserId ADMIN = new UserId(1L);

    @Test
    void submitsPendingReportWithTrimmedDetails() {
        var report = PlayerReport.submit(new MatchId(1L), REPORTER, REPORTED,
                ReportReason.HARASSMENT, "  Insultó al árbitro  ", NOW);

        assertThat(report.getStatus()).isEqualTo(PlayerReportStatus.PENDING);
        assertThat(report.getDetails()).isEqualTo("Insultó al árbitro");
        assertThat(report.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void storesBlankDetailsAsNull() {
        var report = PlayerReport.submit(new MatchId(1L), REPORTER, REPORTED, ReportReason.NO_SHOW, "   ", NOW);

        assertThat(report.getDetails()).isNull();
    }

    @Test
    void rejectsReportingYourself() {
        assertThatThrownBy(() -> PlayerReport.submit(new MatchId(1L), REPORTER, new UserId(7L),
                ReportReason.HARASSMENT, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot report themselves");
    }

    @Test
    void requiresDetailsWhenReasonIsOther() {
        assertThatThrownBy(() -> PlayerReport.submit(new MatchId(1L), REPORTER, REPORTED,
                ReportReason.OTHER, " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDetailsLongerThanLimit() {
        var tooLong = "a".repeat(PlayerReport.MAX_DETAILS_LENGTH + 1);

        assertThatThrownBy(() -> PlayerReport.submit(new MatchId(1L), REPORTER, REPORTED,
                ReportReason.HARASSMENT, tooLong, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesPendingReport() {
        var report = pendingReport();
        var reviewedAt = NOW.plusHours(2);

        report.resolve(ADMIN, PlayerReportStatus.ACTION_TAKEN, "Suspendido 7 días", reviewedAt);

        assertThat(report.getStatus()).isEqualTo(PlayerReportStatus.ACTION_TAKEN);
        assertThat(report.getReviewedBy()).isEqualTo(ADMIN);
        assertThat(report.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(report.getResolutionNote()).isEqualTo("Suspendido 7 días");
    }

    @Test
    void rejectsPendingAsResolutionOutcome() {
        var report = pendingReport();

        assertThatThrownBy(() -> report.resolve(ADMIN, PlayerReportStatus.PENDING, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preventsResolvingAnAlreadyResolvedReport() {
        var report = pendingReport();
        report.resolve(ADMIN, PlayerReportStatus.DISMISSED, null, NOW);

        assertThatThrownBy(() -> report.resolve(ADMIN, PlayerReportStatus.ACTION_TAKEN, null, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PlayerReport pendingReport() {
        return PlayerReport.submit(new MatchId(1L), REPORTER, REPORTED, ReportReason.UNSPORTSMANLIKE_CONDUCT, null, NOW);
    }
}
