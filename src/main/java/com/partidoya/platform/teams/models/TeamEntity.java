package com.partidoya.platform.teams.models;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter @Setter @NoArgsConstructor
public class TeamEntity extends AuditableAbstractPersistenceEntity {
    @Column(nullable = false, length = 80) private String name;
    @Column(name = "normalized_name", nullable = false, unique = true, length = 80) private String normalizedName;
    @Column(nullable = false, length = 50) private String sport;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "logo_url", length = 500) private String logoUrl;
    @Column(name = "logo_public_id", length = 255) private String logoPublicId;
    @Column(length = 80) private String district;
    @Column(name = "competitive_level", nullable = false, length = 20) private String competitiveLevel;
    @Column(name = "open_to_requests", nullable = false) private boolean openToRequests;
    @Column(name = "captain_id", nullable = false) private Long captainId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "max_members", nullable = false) private int maxMembers = 30;
}
