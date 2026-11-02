package com.partidoya.platform.users.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResource(
        Long id,
        String email,
        String fullName,
        boolean emailVerified,
        String role,
        String plan,
        String status,
        String suspensionReason,
        LocalDate suspendedUntil,
        LocalDateTime lastAdministrativeActionAt
) {
}
