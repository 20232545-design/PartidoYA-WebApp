package com.partidoya.platform.users.dto;

import java.time.LocalDate;

public record UserAdministrativeActionRequestResource(
        Long adminId,
        String reason,
        LocalDate suspendedUntil
) {
}
