package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.CourtAvailability;
import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtAvailabilityPersistenceEntity;

public final class CourtAvailabilityPersistenceAssembler {
    private CourtAvailabilityPersistenceAssembler() {
    }

    public static CourtAvailability toDomainFromPersistence(CourtAvailabilityPersistenceEntity entity) {
        if (entity == null) return null;
        return new CourtAvailability(
                new CourtAvailabilityId(entity.getId()),
                new CourtId(entity.getCourtId()),
                entity.getDate(),
                entity.isAllDay(),
                entity.getStartTime(),
                entity.getEndTime(),
                CourtAvailabilityType.valueOf(entity.getType()),
                entity.getReason(),
                entity.getAvailabilityCreatedAt());
    }

    public static CourtAvailabilityPersistenceEntity toPersistenceFromDomain(CourtAvailability availability) {
        if (availability == null) return null;
        var entity = new CourtAvailabilityPersistenceEntity();
        if (availability.getId() != null) entity.setId(availability.getId().value());
        entity.setCourtId(availability.getCourtId().value());
        entity.setDate(availability.getDate());
        entity.setAllDay(availability.isAllDay());
        entity.setStartTime(availability.getStartTime());
        entity.setEndTime(availability.getEndTime());
        entity.setType(availability.getType().name());
        entity.setReason(availability.getReason());
        entity.setAvailabilityCreatedAt(availability.getCreatedAt());
        return entity;
    }
}
