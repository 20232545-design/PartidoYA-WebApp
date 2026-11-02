package com.partidoya.platform.users.models;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.models.UserStatus;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class User {
    private UserId id;
    private EmailAddress email;
    private FullName fullName;
    private HashedPassword password;
    private boolean emailVerified;
    private Role role;
    private Plan plan;
    private UserStatus status;
    private String suspensionReason;
    private LocalDate suspendedUntil;
    private LocalDateTime lastAdministrativeActionAt;
    private UserId lastAdministrativeActionBy;

    protected User() {
    }

    public User(EmailAddress email, FullName fullName, HashedPassword password, Role role) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
        this.emailVerified = false;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.plan = Plan.defaultForRole(this.role);
        this.status = UserStatus.ACTIVE;
    }

    public User(UserId id, EmailAddress email, FullName fullName, HashedPassword password, boolean emailVerified,
                Role role, Plan plan) {
        this(email, fullName, password, role);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.emailVerified = emailVerified;
        this.plan = Objects.requireNonNull(plan, "plan must not be null");
    }

    public User(UserId id, EmailAddress email, FullName fullName, HashedPassword password, boolean emailVerified,
                Role role, Plan plan, UserStatus status, String suspensionReason, LocalDate suspendedUntil,
                LocalDateTime lastAdministrativeActionAt, UserId lastAdministrativeActionBy) {
        this(id, email, fullName, password, emailVerified, role, plan);
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.suspensionReason = normalize(suspensionReason);
        this.suspendedUntil = suspendedUntil;
        this.lastAdministrativeActionAt = lastAdministrativeActionAt;
        this.lastAdministrativeActionBy = lastAdministrativeActionBy;
    }

    public void updateFullName(FullName fullName) {
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
    }

    public void updatePlan(Plan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        if (!plan.isCompatibleWith(this.role)) {
            throw new IllegalArgumentException("Plan '%s' is not available for role '%s'".formatted(plan, this.role));
        }
        this.plan = plan;
    }

    public void activate(UserId adminId) {
        ensureAdminAction(adminId);
        this.status = UserStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedUntil = null;
        this.lastAdministrativeActionAt = LocalDateTime.now();
        this.lastAdministrativeActionBy = adminId;
    }

    public void suspend(UserId adminId, String reason, LocalDate until) {
        ensureAdminAction(adminId);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Suspension reason is required");
        }
        this.status = UserStatus.SUSPENDED;
        this.suspensionReason = reason.trim();
        this.suspendedUntil = until;
        this.lastAdministrativeActionAt = LocalDateTime.now();
        this.lastAdministrativeActionBy = adminId;
    }

    public void reactivate(UserId adminId, String reason) {
        ensureAdminAction(adminId);
        this.status = UserStatus.ACTIVE;
        this.suspensionReason = normalize(reason);
        this.suspendedUntil = null;
        this.lastAdministrativeActionAt = LocalDateTime.now();
        this.lastAdministrativeActionBy = adminId;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static void ensureAdminAction(UserId adminId) {
        Objects.requireNonNull(adminId, "adminId must not be null");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
