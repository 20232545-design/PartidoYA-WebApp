package com.partidoya.platform.auth.security;

import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public AuthenticatedUser require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ForbiddenActionException("Authentication is required");
        }
        return principal;
    }

    public UserId userId() {
        return new UserId(require().userId());
    }

    public UserId requireSelf(Long requestedUserId) {
        var current = require();
        if (!current.userId().equals(requestedUserId) && current.role() != Role.ADMIN_GENERAL) {
            throw new ForbiddenActionException("You cannot access another user's account");
        }
        return new UserId(requestedUserId);
    }

    public UserId requireRole(Role role) {
        var current = require();
        if (current.role() != role) {
            throw new ForbiddenActionException("This action is not available for your account");
        }
        return new UserId(current.userId());
    }
}
