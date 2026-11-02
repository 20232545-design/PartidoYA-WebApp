package com.partidoya.platform.users.dto;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.dto.UserResource;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.isEmailVerified(),
                user.getRole().name(),
                user.getPlan().name(),
                user.getStatus().name(),
                user.getSuspensionReason(),
                user.getSuspendedUntil(),
                user.getLastAdministrativeActionAt());
    }
}
