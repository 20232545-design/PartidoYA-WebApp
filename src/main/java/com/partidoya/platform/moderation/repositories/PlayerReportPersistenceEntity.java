package com.partidoya.platform.moderation.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_reports")
@Getter
@Setter
@NoArgsConstructor
public class PlayerReportPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_player_id", nullable = false)
    private Long reportedPlayerId;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}
