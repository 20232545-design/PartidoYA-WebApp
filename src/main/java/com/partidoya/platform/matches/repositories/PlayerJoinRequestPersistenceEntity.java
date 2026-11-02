package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_join_requests")
@Getter
@Setter
@NoArgsConstructor
public class PlayerJoinRequestPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "proof_storage_key", nullable = false, length = 500)
    private String proofStorageKey;

    @Column(name = "original_file_name", length = 200)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
