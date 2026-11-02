package com.partidoya.platform.users.models;

import java.util.List;

public record AdminUserDetailView(
        AdminUserView user,
        List<AdminUserActionView> actions
) {
}
