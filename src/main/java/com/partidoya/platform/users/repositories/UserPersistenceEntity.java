package com.partidoya.platform.users.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 30)
    private String plan;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "suspension_reason", length = 300)
    private String suspensionReason;

    @Column(name = "suspended_until")
    private LocalDate suspendedUntil;

    @Column(name = "last_administrative_action_at")
    private LocalDateTime lastAdministrativeActionAt;

    @Column(name = "last_administrative_action_by")
    private Long lastAdministrativeActionBy;
}
