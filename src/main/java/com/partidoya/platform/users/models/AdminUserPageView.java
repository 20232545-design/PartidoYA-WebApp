package com.partidoya.platform.users.models;

import java.util.List;

public record AdminUserPageView(
        List<AdminUserView> content,
        long totalElements,
        int page,
        int size
) {
}
