package com.partidoya.platform.courts.dto;

import com.partidoya.platform.courts.models.ManagedReservationView;
import com.partidoya.platform.courts.dto.ManagedReservationResource;

public final class ManagedReservationResourceFromEntityAssembler {
    private ManagedReservationResourceFromEntityAssembler() {
    }

    public static ManagedReservationResource toResourceFromEntity(ManagedReservationView reservation) {
        return new ManagedReservationResource(
                reservation.id(),
                reservation.userId(),
                reservation.customerName(),
                reservation.courtId(),
                reservation.courtName(),
                reservation.date(),
                reservation.startTime(),
                reservation.endTime(),
                reservation.price(),
                reservation.currency(),
                reservation.providerReference(),
                reservation.status(),
                reservation.paymentStatus(),
                reservation.createdAt());
    }
}
