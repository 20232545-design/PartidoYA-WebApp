package com.partidoya.platform.users.dto;

import com.partidoya.platform.users.models.AdminUserActionView;
import com.partidoya.platform.users.models.AdminUserDetailView;
import com.partidoya.platform.users.models.AdminUserPageView;
import com.partidoya.platform.users.models.AdminUserView;
import com.partidoya.platform.users.dto.AdminUserActionResource;
import com.partidoya.platform.users.dto.AdminUserDetailResource;
import com.partidoya.platform.users.dto.AdminUserPageResource;
import com.partidoya.platform.users.dto.AdminUserResource;

public final class AdminUserResourceFromEntityAssembler {
    private AdminUserResourceFromEntityAssembler() {
    }

    public static AdminUserPageResource toResourceFromEntity(AdminUserPageView page) {
        return new AdminUserPageResource(
                page.content().stream().map(AdminUserResourceFromEntityAssembler::toUser).toList(),
                page.totalElements(),
                page.page(),
                page.size());
    }

    public static AdminUserDetailResource toResourceFromEntity(AdminUserDetailView detail) {
        return new AdminUserDetailResource(
                toUser(detail.user()),
                detail.actions().stream().map(AdminUserResourceFromEntityAssembler::toAction).toList());
    }

    private static AdminUserResource toUser(AdminUserView user) {
        return new AdminUserResource(
                user.id(),
                user.email(),
                user.fullName(),
                user.role(),
                user.plan(),
                user.status(),
                user.suspensionReason(),
                user.suspendedUntil(),
                user.lastAdministrativeActionAt(),
                user.reservationCount(),
                user.matchCount(),
                user.averageRating());
    }

    private static AdminUserActionResource toAction(AdminUserActionView action) {
        return new AdminUserActionResource(action.id(), action.adminId(), action.action(), action.reason(), action.createdAt());
    }
}
