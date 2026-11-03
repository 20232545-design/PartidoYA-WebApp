package com.partidoya.platform.auth.security;

import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;

public record AuthenticatedUser(Long userId, String email, Role role, Plan plan) {
}
