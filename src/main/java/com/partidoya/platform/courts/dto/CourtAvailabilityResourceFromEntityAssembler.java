package com.partidoya.platform.courts.dto;

import com.partidoya.platform.courts.models.CourtAvailabilityView;
import com.partidoya.platform.courts.dto.CourtAvailabilityResource;

public final class CourtAvailabilityResourceFromEntityAssembler {
    private CourtAvailabilityResourceFromEntityAssembler() {
    }

    public static CourtAvailabilityResource toResourceFromEntity(CourtAvailabilityView availability) {
        return new CourtAvailabilityResource(
                availability.id(),
                availability.courtId(),
                availability.courtName(),
                availability.date(),
                availability.allDay(),
                availability.startTime(),
                availability.endTime(),
                availability.type(),
                availability.reason(),
                availability.createdAt());
    }
}
