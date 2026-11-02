package com.partidoya.platform.courts.dto;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.dto.CourtResource;

public final class CourtResourceFromEntityAssembler {
    private CourtResourceFromEntityAssembler() {
    }

    public static CourtResource toResourceFromEntity(Court court) {
        return new CourtResource(
                court.getId() != null ? court.getId().value() : null,
                court.getOwnerId().value(),
                court.getName(),
                court.getComplexName(),
                court.getDescription(),
                court.getAddress(),
                court.getDistrict(),
                court.getLatitude(),
                court.getLongitude(),
                court.getPricePerHour(),
                court.isActive(),
                court.isPublished(),
                court.isAvailableForReservations(),
                court.getSports(),
                court.getSchedules(),
                court.getMainImageUrl(),
                court.getImageUrls(),
                court.getServices(),
                court.getFeatures());
    }
}
