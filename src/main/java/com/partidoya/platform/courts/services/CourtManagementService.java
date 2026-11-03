package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtRepository;
import com.partidoya.platform.users.models.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourtManagementService {
    private final CourtRepository courtRepository;
    private final CourtAccessPolicy accessPolicy;

    public CourtManagementService(CourtRepository courtRepository, CourtAccessPolicy accessPolicy) {
        this.courtRepository = courtRepository;
        this.accessPolicy = accessPolicy;
    }

    public Court create(UserId ownerId, CourtDetails details) {
        accessPolicy.requireCourtCreation(ownerId);
        var court = new Court(
                ownerId,
                details.name(),
                details.complexName(),
                details.description(),
                details.address(),
                details.district(),
                details.latitude(),
                details.longitude(),
                details.pricePerHour(),
                details.sports(),
                details.schedules(),
                details.mainImageUrl(),
                details.imageUrls(),
                details.services(),
                details.features());
        court.updateVenueMetadata(details.googlePlaceId(), details.googlePlaceName(), details.surfaceType(),
                details.capacity(), details.covered());
        return courtRepository.save(court);
    }

    public Court update(CourtId courtId, UserId requesterId, CourtDetails details) {
        var court = accessPolicy.requireOwnedCourt(courtId, requesterId);
        court.updateDetails(
                details.name(),
                details.complexName(),
                details.description(),
                details.address(),
                details.district(),
                details.latitude(),
                details.longitude(),
                details.pricePerHour(),
                details.sports(),
                details.schedules(),
                details.mainImageUrl(),
                details.imageUrls(),
                details.services(),
                details.features());
        court.updateVenueMetadata(details.googlePlaceId(), details.googlePlaceName(), details.surfaceType(),
                details.capacity(), details.covered());
        return courtRepository.save(court);
    }

    public Court setPublication(CourtId courtId, UserId requesterId, boolean published) {
        var court = accessPolicy.requireOwnedCourt(courtId, requesterId);
        if (published) court.publish();
        else court.unpublish();
        return courtRepository.save(court);
    }

    @Transactional(readOnly = true)
    public List<Court> listManagedCourts(UserId ownerId) {
        return accessPolicy.managedCourts(ownerId, null);
    }
}
