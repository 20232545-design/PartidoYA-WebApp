package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.CourtAvailability;
import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourtAvailabilityRepository {
    Optional<CourtAvailability> findById(CourtAvailabilityId id);
    List<CourtAvailability> findByCourtIdAndDate(CourtId courtId, LocalDate date);
    List<CourtAvailability> findByCourtIds(Collection<CourtId> courtIds, LocalDate from, LocalDate to);
    boolean existsOverlapping(CourtId courtId, LocalDate date, java.time.LocalTime startTime, java.time.LocalTime endTime,
                              CourtAvailabilityId excludedId);
    boolean existsOverlappingType(CourtId courtId, LocalDate date, java.time.LocalTime startTime, java.time.LocalTime endTime,
                                  CourtAvailabilityType type);
    CourtAvailability save(CourtAvailability availability);
    void delete(CourtAvailability availability);
}
