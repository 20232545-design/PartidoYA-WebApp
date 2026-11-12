package com.partidoya.platform.teams.models;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class TeamMembershipEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "team_id", nullable = false) private Long teamId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "joined_at", nullable = false) private LocalDateTime joinedAt;
    @Column(name = "ended_at") private LocalDateTime endedAt;
}
