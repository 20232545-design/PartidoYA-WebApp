package com.partidoya.platform.iam.application.internal.queryservices;

import com.partidoya.platform.iam.application.queryservices.UserQueryService;
import com.partidoya.platform.iam.application.queryservices.AdminUserActionView;
import com.partidoya.platform.iam.application.queryservices.AdminUserDetailView;
import com.partidoya.platform.iam.application.queryservices.AdminUserPageView;
import com.partidoya.platform.iam.application.queryservices.AdminUserView;
import com.partidoya.platform.iam.domain.model.aggregates.User;
import com.partidoya.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.partidoya.platform.iam.domain.model.queries.GetManagedUserDetailQuery;
import com.partidoya.platform.iam.domain.model.queries.SearchManagedUsersQuery;
import com.partidoya.platform.iam.domain.model.valueobjects.Role;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import com.partidoya.platform.iam.domain.repositories.UserAdministrativeActionRepository;
import com.partidoya.platform.iam.domain.repositories.UserRepository;
import com.partidoya.platform.iam.domain.services.PlanPolicy;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.matches.domain.repositories.MatchRepository;
import com.partidoya.platform.shared.domain.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;
    private final UserAdministrativeActionRepository actionRepository;
    private final ReservationRepository reservationRepository;
    private final MatchRepository matchRepository;

    public UserQueryServiceImpl(UserRepository userRepository, UserAdministrativeActionRepository actionRepository,
                                ReservationRepository reservationRepository, MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.reservationRepository = reservationRepository;
        this.matchRepository = matchRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public AdminUserPageView handle(SearchManagedUsersQuery query) {
        ensurePlatformAdmin(query.adminId());
        var role = roleFilter(query.roleFilter());
        var page = Math.max(query.page(), 0);
        var size = Math.min(Math.max(query.size(), 1), 50);
        var users = userRepository.search(query.search(), role, page, size).stream()
                .map(this::toAdminUserView)
                .toList();
        var total = userRepository.countSearch(query.search(), role);
        return new AdminUserPageView(users, total, page, size);
    }

    @Override
    public AdminUserDetailView handle(GetManagedUserDetailQuery query) {
        ensurePlatformAdmin(query.adminId());
        var user = userRepository.findById(query.targetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", query.targetUserId().value().toString()));
        var actions = actionRepository.findByTargetUserId(query.targetUserId()).stream()
                .map(action -> new AdminUserActionView(action.getId(), action.getAdminId().value(), action.getAction(),
                        action.getReason(), action.getCreatedAt()))
                .toList();
        return new AdminUserDetailView(toAdminUserView(user), actions);
    }

    private AdminUserView toAdminUserView(User user) {
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

    private void ensurePlatformAdmin(UserId adminId) {
        var admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId.value().toString()));
        if (!PlanPolicy.canManagePlatform(admin)) {
            throw new ForbiddenActionException("Only platform administrators can manage users");
        }
    }

    private static Role roleFilter(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank() || roleFilter.equalsIgnoreCase("ALL")) return null;
        if (roleFilter.equalsIgnoreCase("PLAYERS")) return Role.JUGADOR;
        if (roleFilter.equalsIgnoreCase("COURT_ADMINS")) return Role.ADMIN_CANCHA;
        return Role.valueOf(roleFilter);
    }
}
