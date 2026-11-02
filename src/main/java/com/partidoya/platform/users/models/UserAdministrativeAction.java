package com.partidoya.platform.users.models;

import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class UserAdministrativeAction {
    private Long id;
    private UserId adminId;
    private UserId targetUserId;
    private String action;
    private String reason;
    private LocalDateTime createdAt;

    protected UserAdministrativeAction() {
    }

    public UserAdministrativeAction(UserId adminId, UserId targetUserId, String action, String reason) {
        this.adminId = Objects.requireNonNull(adminId, "adminId must not be null");
        this.targetUserId = Objects.requireNonNull(targetUserId, "targetUserId must not be null");
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Action is required");
        this.action = action.trim();
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.createdAt = LocalDateTime.now();
    }

    public UserAdministrativeAction(Long id, UserId adminId, UserId targetUserId, String action, String reason,
                                    LocalDateTime createdAt) {
        this(adminId, targetUserId, action, reason);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
