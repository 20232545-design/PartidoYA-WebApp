package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.ReservationId;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Optional<Reservation> findById(ReservationId id);
    boolean existsOverlapping(CourtId courtId, LocalDate date, LocalTime startTime, LocalTime endTime);
    boolean existsByPaymentIdempotencyKey(String paymentIdempotencyKey);
    boolean existsByProviderReference(String providerReference);
    List<Reservation> findByCourtId(CourtId courtId);
    List<Reservation> findByCourtIdAndDate(CourtId courtId, LocalDate date);
    List<Reservation> findByCourtIds(Collection<CourtId> courtIds, LocalDate from, LocalDate to);
    long countByUserId(com.partidoya.platform.users.models.UserId userId);
    Reservation save(Reservation reservation);
}
