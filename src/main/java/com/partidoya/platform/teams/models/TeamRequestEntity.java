package com.partidoya.platform.teams.models;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_requests")
@Getter @Setter @NoArgsConstructor
public class TeamRequestEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "team_id", nullable = false) private Long teamId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "requested_by", nullable = false) private Long requestedBy;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
}
