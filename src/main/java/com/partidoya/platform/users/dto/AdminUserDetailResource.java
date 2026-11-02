package com.partidoya.platform.users.dto;

import java.util.List;

public record AdminUserDetailResource(AdminUserResource user, List<AdminUserActionResource> actions) {
}
