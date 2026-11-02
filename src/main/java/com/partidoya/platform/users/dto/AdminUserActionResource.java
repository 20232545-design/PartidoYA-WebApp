package com.partidoya.platform.users.dto;

import java.time.LocalDateTime;

public record AdminUserActionResource(
        Long id,
        Long adminId,
        String action,
        String reason,
        LocalDateTime createdAt
) {
}
