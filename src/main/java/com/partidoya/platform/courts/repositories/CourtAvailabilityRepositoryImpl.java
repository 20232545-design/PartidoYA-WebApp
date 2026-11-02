package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.CourtAvailability;
import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtAvailabilityRepository;
import com.partidoya.platform.courts.repositories.CourtAvailabilityPersistenceAssembler;
import com.partidoya.platform.courts.repositories.CourtAvailabilityPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class CourtAvailabilityRepositoryImpl implements CourtAvailabilityRepository {
    private final CourtAvailabilityPersistenceRepository repository;

    public CourtAvailabilityRepositoryImpl(CourtAvailabilityPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CourtAvailability> findById(CourtAvailabilityId id) {
        return repository.findById(id.value()).map(CourtAvailabilityPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<CourtAvailability> findByCourtIdAndDate(CourtId courtId, LocalDate date) {
        return repository.findByCourtIdAndDateOrderByStartTimeAsc(courtId.value(), date).stream()
                .map(CourtAvailabilityPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<CourtAvailability> findByCourtIds(Collection<CourtId> courtIds, LocalDate from, LocalDate to) {
        var ids = courtIds.stream().map(CourtId::value).toList();
        if (ids.isEmpty()) return List.of();
        return repository.findByCourtIdInAndDateBetweenOrderByDateAscStartTimeAsc(ids, from, to).stream()
                .map(CourtAvailabilityPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public boolean existsOverlapping(CourtId courtId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                     CourtAvailabilityId excludedId) {
        return findByCourtIdAndDate(courtId, date).stream()
                .filter(item -> excludedId == null || !item.getId().value().equals(excludedId.value()))
                .anyMatch(item -> item.overlaps(date, startTime, endTime));
    }

    @Override
    public boolean existsOverlappingType(CourtId courtId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                         CourtAvailabilityType type) {
        return findByCourtIdAndDate(courtId, date).stream()
                .filter(item -> item.getType() == type)
                .anyMatch(item -> item.overlaps(date, startTime, endTime));
    }

    @Override
    public CourtAvailability save(CourtAvailability availability) {
        return CourtAvailabilityPersistenceAssembler.toDomainFromPersistence(
                repository.save(CourtAvailabilityPersistenceAssembler.toPersistenceFromDomain(availability)));
    }

    @Override
    public void delete(CourtAvailability availability) {
        repository.deleteById(availability.getId().value());
    }
}
