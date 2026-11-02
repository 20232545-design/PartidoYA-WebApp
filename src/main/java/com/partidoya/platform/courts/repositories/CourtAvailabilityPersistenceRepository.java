package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.repositories.CourtAvailabilityPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CourtAvailabilityPersistenceRepository extends JpaRepository<CourtAvailabilityPersistenceEntity, Long> {
    List<CourtAvailabilityPersistenceEntity> findByCourtIdAndDateOrderByStartTimeAsc(Long courtId, LocalDate date);

    List<CourtAvailabilityPersistenceEntity> findByCourtIdInAndDateBetweenOrderByDateAscStartTimeAsc(
            Collection<Long> courtIds, LocalDate from, LocalDate to);
}
