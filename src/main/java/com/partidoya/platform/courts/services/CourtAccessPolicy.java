package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtRepository;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import com.partidoya.platform.iam.domain.repositories.UserRepository;
import com.partidoya.platform.iam.domain.services.PlanPolicy;
import com.partidoya.platform.shared.domain.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourtAccessPolicy {
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    public CourtAccessPolicy(CourtRepository courtRepository, UserRepository userRepository) {
        this.courtRepository = courtRepository;
        this.userRepository = userRepository;
    }

    public void requireCourtManager(UserId userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
        if (!PlanPolicy.canManageCourts(user)) {
            throw new ForbiddenActionException("Only court plan accounts can manage courts");
        }
    }

    public Court requireOwnedCourt(CourtId courtId, UserId ownerId) {
        requireCourtManager(ownerId);
        var court = requireCourt(courtId);
        if (!court.getOwnerId().value().equals(ownerId.value())) {
            throw new ForbiddenActionException("Only the court owner can modify this court");
        }
        return court;
    }

    public Court requireReservableCourt(CourtId courtId) {
        var court = requireCourt(courtId);
        if (!isEligibleForReservations(court)) {
            throw new ForbiddenActionException("Court is not available for reservations");
        }
        return court;
    }

    public List<Court> managedCourts(UserId ownerId, CourtId courtId) {
        requireCourtManager(ownerId);
        var courts = courtRepository.findByOwnerId(ownerId);
        if (courtId == null) return courts;
        var selectedCourts = courts.stream()
                .filter(court -> court.getId().value().equals(courtId.value()))
                .toList();
        if (selectedCourts.isEmpty()) {
            throw new ForbiddenActionException("You cannot access this court");
        }
        return selectedCourts;
    }

    public boolean isEligibleForReservations(Court court) {
        return court.canReceiveReservations()
                && userRepository.findById(court.getOwnerId()).map(PlanPolicy::canManageCourts).orElse(false);
    }

    private Court requireCourt(CourtId courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId.value().toString()));
    }
}
