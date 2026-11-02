package com.partidoya.platform.users.models;

import java.time.LocalDateTime;

public record AdminUserActionView(
        Long id,
        Long adminId,
        String action,
        String reason,
        LocalDateTime createdAt
) {
}
