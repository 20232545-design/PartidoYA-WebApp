package com.partidoya.platform.users.services;

import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.users.models.AdminUserActionView;
import com.partidoya.platform.users.models.AdminUserDetailView;
import com.partidoya.platform.users.models.AdminUserPageView;
import com.partidoya.platform.users.models.AdminUserView;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserAdministrativeAction;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserAdministrativeActionRepository;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.users.services.PlanPolicy;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class UserAdministrationService {
    private final UserRepository userRepository;
    private final UserAdministrativeActionRepository actionRepository;
    private final ReservationRepository reservationRepository;
    private final MatchRepository matchRepository;

    public UserAdministrationService(
            UserRepository userRepository,
            UserAdministrativeActionRepository actionRepository,
            ReservationRepository reservationRepository,
            MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.reservationRepository = reservationRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public AdminUserPageView search(
            UserId adminId,
            String search,
            String roleFilter,
            int pageNumber,
            int pageSize) {
        requirePlatformAdmin(adminId);
        var role = resolveRole(roleFilter);
        var page = Math.max(pageNumber, 0);
        var size = Math.min(Math.max(pageSize, 1), 50);
        var users = userRepository.search(search, role, page, size).stream()
                .map(this::toAdminView)
                .toList();
        return new AdminUserPageView(users, userRepository.countSearch(search, role), page, size);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailView findDetail(UserId adminId, UserId targetUserId) {
        requirePlatformAdmin(adminId);
        var user = requireUser(targetUserId);
        var actions = actionRepository.findByTargetUserId(targetUserId).stream()
                .map(action -> new AdminUserActionView(
                        action.getId(),
                        action.getAdminId().value(),
                        action.getAction(),
                        action.getReason(),
                        action.getCreatedAt()))
                .toList();
        return new AdminUserDetailView(toAdminView(user), actions);
    }

    public User activate(UserId adminId, UserId targetUserId, String reason) {
        requirePlatformAdmin(adminId);
        var user = requireUser(targetUserId);
        user.activate(adminId);
        recordAction(adminId, targetUserId, "ACTIVATE", reason);
        return userRepository.save(user);
    }

    public User suspend(
            UserId adminId,
            UserId targetUserId,
            String reason,
            LocalDate suspendedUntil) {
        requirePlatformAdmin(adminId);
        if (adminId.value().equals(targetUserId.value())) {
            throw new IllegalArgumentException("Admin cannot suspend own account");
        }
        var user = requireUser(targetUserId);
        user.suspend(adminId, reason, suspendedUntil);
        recordAction(adminId, targetUserId, "SUSPEND", reason);
        return userRepository.save(user);
    }

    public User reactivate(UserId adminId, UserId targetUserId, String reason) {
        requirePlatformAdmin(adminId);
        var user = requireUser(targetUserId);
        user.reactivate(adminId, reason);
        recordAction(adminId, targetUserId, "REACTIVATE", reason);
        return userRepository.save(user);
    }

    private void recordAction(UserId adminId, UserId targetUserId, String action, String reason) {
        actionRepository.save(new UserAdministrativeAction(adminId, targetUserId, action, reason));
    }

    private User requireUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
    }

    private void requirePlatformAdmin(UserId adminId) {
        if (!PlanPolicy.canManagePlatform(requireUser(adminId))) {
            throw new ForbiddenActionException("Only platform administrators can manage users");
        }
    }

    private AdminUserView toAdminView(User user) {
        return new AdminUserView(
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getRole().name(),
                user.getPlan().name(),
                user.getStatus().name(),
                user.getSuspensionReason(),
                user.getSuspendedUntil(),
                user.getLastAdministrativeActionAt(),
                reservationRepository.countByUserId(user.getId()),
                matchRepository.countRelatedToUser(user.getId()),
                null);
    }

    private Role resolveRole(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank() || roleFilter.equalsIgnoreCase("ALL")) return null;
        if (roleFilter.equalsIgnoreCase("PLAYERS")) return Role.JUGADOR;
        if (roleFilter.equalsIgnoreCase("COURT_ADMINS")) return Role.ADMIN_CANCHA;
        return Role.valueOf(roleFilter);
    }
}
