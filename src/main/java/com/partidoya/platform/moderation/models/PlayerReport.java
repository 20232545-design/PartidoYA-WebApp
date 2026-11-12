package com.partidoya.platform.moderation.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Reporte de un jugador sobre otro jugador con quien compartió un partido.
 * Las reglas que no dependen de otros agregados (no auto-reportarse, longitud de textos,
 * ciclo PENDING → resuelto) viven aquí; las que requieren el partido viven en el servicio.
 */
@Getter
public class PlayerReport {
    public static final int MAX_DETAILS_LENGTH = 1000;
    public static final int MAX_RESOLUTION_NOTE_LENGTH = 500;

    private final Long id;
    private final MatchId matchId;
    private final UserId reporterId;
    private final UserId reportedPlayerId;
    private final ReportReason reason;
    private final String details;
    private final LocalDateTime submittedAt;
    private PlayerReportStatus status;
    private LocalDateTime reviewedAt;
    private UserId reviewedBy;
    private String resolutionNote;

    private PlayerReport(Long id, MatchId matchId, UserId reporterId, UserId reportedPlayerId, ReportReason reason,
                         String details, PlayerReportStatus status, LocalDateTime submittedAt,
                         LocalDateTime reviewedAt, UserId reviewedBy, String resolutionNote) {
        this.id = id;
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.reporterId = Objects.requireNonNull(reporterId, "reporterId must not be null");
        this.reportedPlayerId = Objects.requireNonNull(reportedPlayerId, "reportedPlayerId must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.details = details;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.resolutionNote = resolutionNote;
    }

    /** Crea un reporte nuevo en estado {@link PlayerReportStatus#PENDING}. */
    public static PlayerReport submit(MatchId matchId, UserId reporterId, UserId reportedPlayerId,
                                      ReportReason reason, String details, LocalDateTime now) {
        Objects.requireNonNull(reporterId, "reporterId must not be null");
        Objects.requireNonNull(reportedPlayerId, "reportedPlayerId must not be null");
        if (reporterId.value().equals(reportedPlayerId.value())) {
            throw new IllegalArgumentException("A player cannot report themselves");
        }
        var normalizedDetails = normalize(details, MAX_DETAILS_LENGTH, "details");
        if (reason == ReportReason.OTHER && normalizedDetails == null) {
            throw new IllegalArgumentException("details are required when the reason is OTHER");
        }
        return new PlayerReport(null, matchId, reporterId, reportedPlayerId, reason, normalizedDetails,
                PlayerReportStatus.PENDING, now, null, null, null);
    }

    /** Reconstruye un reporte ya persistido, sin volver a aplicar las reglas de creación. */
    public static PlayerReport restore(Long id, MatchId matchId, UserId reporterId, UserId reportedPlayerId,
                                       ReportReason reason, String details, PlayerReportStatus status,
                                       LocalDateTime submittedAt, LocalDateTime reviewedAt, UserId reviewedBy,
                                       String resolutionNote) {
        return new PlayerReport(Objects.requireNonNull(id, "id must not be null"), matchId, reporterId,
                reportedPlayerId, reason, details, status, submittedAt, reviewedAt, reviewedBy, resolutionNote);
    }

    public void resolve(UserId adminId, PlayerReportStatus outcome, String note, LocalDateTime now) {
        Objects.requireNonNull(adminId, "adminId must not be null");
        if (outcome == null || !outcome.isResolution()) {
            throw new IllegalArgumentException("outcome must be ACTION_TAKEN or DISMISSED");
        }
        if (status != PlayerReportStatus.PENDING) {
            throw new IllegalStateException("Only pending reports can be resolved");
        }
        this.resolutionNote = normalize(note, MAX_RESOLUTION_NOTE_LENGTH, "note");
        this.status = outcome;
        this.reviewedBy = adminId;
        this.reviewedAt = Objects.requireNonNull(now, "now must not be null");
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) return null;
        var trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("%s must not exceed %d characters".formatted(field, maxLength));
        }
        return trimmed;
    }
}
