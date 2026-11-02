package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.PaymentStatus;
import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.models.ReservationStatus;
import com.partidoya.platform.courts.repositories.ReservationPersistenceEntity;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;

public final class ReservationPersistenceAssembler {
    private ReservationPersistenceAssembler() {
    }

    public static Reservation toDomainFromPersistence(ReservationPersistenceEntity entity) {
        if (entity == null) return null;
        return new Reservation(
                new ReservationId(entity.getId()),
                new UserId(entity.getUserId()),
                new CourtId(entity.getCourtId()),
                entity.getDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getProviderReference(),
                entity.getPaymentIdempotencyKey(),
                ReservationStatus.valueOf(entity.getStatus()),
                PaymentStatus.valueOf(entity.getPaymentStatus()),
                entity.getReservationCreatedAt());
    }

    public static ReservationPersistenceEntity toPersistenceFromDomain(Reservation reservation) {
        if (reservation == null) return null;
        var entity = new ReservationPersistenceEntity();
        if (reservation.getId() != null) entity.setId(reservation.getId().value());
        entity.setUserId(reservation.getUserId().value());
        entity.setCourtId(reservation.getCourtId().value());
        entity.setDate(reservation.getDate());
        entity.setStartTime(reservation.getStartTime());
        entity.setEndTime(reservation.getEndTime());
        entity.setPrice(reservation.getPrice());
        entity.setCurrency(reservation.getCurrency());
        entity.setProviderReference(reservation.getProviderReference());
        entity.setPaymentIdempotencyKey(reservation.getPaymentIdempotencyKey());
        entity.setStatus(reservation.getStatus().name());
        entity.setPaymentStatus(reservation.getPaymentStatus().name());
        entity.setReservationCreatedAt(reservation.getCreatedAt());
        return entity;
    }
}
