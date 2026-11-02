package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtPersistenceEntity;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;

import java.util.ArrayList;

public final class CourtPersistenceAssembler {
    private CourtPersistenceAssembler() {
    }

    public static Court toDomainFromPersistence(CourtPersistenceEntity entity) {
        if (entity == null) return null;
        return new Court(
                new CourtId(entity.getId()),
                new UserId(entity.getOwnerId()),
                entity.getName(),
                entity.getComplexName(),
                entity.getDescription(),
                entity.getAddress(),
                entity.getDistrict(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getPricePerHour(),
                entity.isActive(),
                entity.isPublished(),
                entity.isAvailableForReservations(),
                entity.getSports(),
                entity.getSchedules(),
                entity.getMainImageUrl(),
                entity.getImageUrls(),
                entity.getServices(),
                entity.getFeatures());
    }

    public static CourtPersistenceEntity toPersistenceFromDomain(Court court) {
        if (court == null) return null;
        var entity = new CourtPersistenceEntity();
        if (court.getId() != null) entity.setId(court.getId().value());
        entity.setOwnerId(court.getOwnerId().value());
        entity.setName(court.getName());
        entity.setComplexName(court.getComplexName());
        entity.setDescription(court.getDescription());
        entity.setAddress(court.getAddress());
        entity.setDistrict(court.getDistrict());
        entity.setLatitude(court.getLatitude());
        entity.setLongitude(court.getLongitude());
        entity.setPricePerHour(court.getPricePerHour());
        entity.setActive(court.isActive());
        entity.setPublished(court.isPublished());
        entity.setAvailableForReservations(court.isAvailableForReservations());
        entity.setSports(new ArrayList<>(court.getSports()));
        entity.setSchedules(new ArrayList<>(court.getSchedules()));
        entity.setMainImageUrl(court.getMainImageUrl());
        entity.setImageUrls(new ArrayList<>(court.getImageUrls()));
        entity.setServices(new ArrayList<>(court.getServices()));
        entity.setFeatures(new ArrayList<>(court.getFeatures()));
        return entity;
    }
}
