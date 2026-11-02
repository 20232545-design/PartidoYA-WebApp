package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.repositories.ReservationPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservationPersistenceRepository extends JpaRepository<ReservationPersistenceEntity, Long> {
    boolean existsByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThanAndStatusIn(
            Long courtId, LocalDate date, LocalTime endTime, LocalTime startTime, Collection<String> statuses);

    boolean existsByPaymentIdempotencyKey(String paymentIdempotencyKey);

    boolean existsByProviderReference(String providerReference);

    List<ReservationPersistenceEntity> findByCourtId(Long courtId);

    List<ReservationPersistenceEntity> findByCourtIdAndDateOrderByStartTimeAsc(Long courtId, LocalDate date);

    List<ReservationPersistenceEntity> findByCourtIdInAndDateBetweenOrderByDateAscStartTimeAsc(
            Collection<Long> courtIds, LocalDate from, LocalDate to);

    long countByUserId(Long userId);
}
