package com.partidoya.platform.users.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserView(
        Long id,
        String email,
        String fullName,
        String role,
        String plan,
        String status,
        String suspensionReason,
        LocalDate suspendedUntil,
        LocalDateTime lastAdministrativeActionAt,
        Long reservationCount,
        Long matchCount,
        Double averageRating
) {
}
