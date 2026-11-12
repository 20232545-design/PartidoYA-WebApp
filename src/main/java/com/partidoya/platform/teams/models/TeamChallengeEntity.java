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
@Table(name = "team_challenges")
@Getter @Setter @NoArgsConstructor
public class TeamChallengeEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "challenger_team_id", nullable = false) private Long challengerTeamId;
    @Column(name = "challenged_team_id", nullable = false) private Long challengedTeamId;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, length = 50) private String sport;
    @Column(length = 300) private String message;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "match_id") private Long matchId;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
}
