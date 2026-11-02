package com.partidoya.platform.users.dto;

import java.util.List;

public record AdminUserPageResource(
        List<AdminUserResource> content,
        long totalElements,
        int page,
        int size
) {
}
