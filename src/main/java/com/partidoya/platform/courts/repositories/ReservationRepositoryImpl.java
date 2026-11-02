package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.courts.repositories.ReservationPersistenceAssembler;
import com.partidoya.platform.courts.repositories.ReservationPersistenceRepository;
import com.partidoya.platform.users.models.UserId;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationPersistenceRepository reservationPersistenceRepository;

    public ReservationRepositoryImpl(ReservationPersistenceRepository reservationPersistenceRepository) {
        this.reservationPersistenceRepository = reservationPersistenceRepository;
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return reservationPersistenceRepository.findById(id.value())
                .map(ReservationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsOverlapping(CourtId courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return reservationPersistenceRepository.existsByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThanAndStatusIn(
                courtId.value(), date, endTime, startTime, List.of("CONFIRMED"));
    }

    @Override
    public boolean existsByPaymentIdempotencyKey(String paymentIdempotencyKey) {
        return paymentIdempotencyKey != null && !paymentIdempotencyKey.isBlank()
                && reservationPersistenceRepository.existsByPaymentIdempotencyKey(paymentIdempotencyKey);
    }

    @Override
    public boolean existsByProviderReference(String providerReference) {
        return providerReference != null && !providerReference.isBlank()
                && reservationPersistenceRepository.existsByProviderReference(providerReference);
    }

    @Override
    public List<Reservation> findByCourtId(CourtId courtId) {
        return reservationPersistenceRepository.findByCourtId(courtId.value()).stream()
                .map(ReservationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Reservation> findByCourtIdAndDate(CourtId courtId, LocalDate date) {
        return reservationPersistenceRepository.findByCourtIdAndDateOrderByStartTimeAsc(courtId.value(), date).stream()
                .map(ReservationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Reservation> findByCourtIds(Collection<CourtId> courtIds, LocalDate from, LocalDate to) {
        var ids = courtIds.stream().map(CourtId::value).toList();
        if (ids.isEmpty()) return List.of();
        return reservationPersistenceRepository.findByCourtIdInAndDateBetweenOrderByDateAscStartTimeAsc(ids, from, to)
                .stream()
                .map(ReservationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public long countByUserId(UserId userId) {
        return reservationPersistenceRepository.countByUserId(userId.value());
    }

    @Override
    public Reservation save(Reservation reservation) {
        var entity = ReservationPersistenceAssembler.toPersistenceFromDomain(reservation);
        return ReservationPersistenceAssembler.toDomainFromPersistence(reservationPersistenceRepository.save(entity));
    }
}
