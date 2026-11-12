package com.partidoya.platform.matches.dto;

import java.util.List;
import java.util.Map;

public record MatchResource(
        Long id,
        Long organizerId,
        String sport,
        String title,
        String description,
        String address,
        String matchDate,
        int totalSlots,
        int availableSlots,
        Double price,
        Double latitude,
        Double longitude,
        String status,
        List<Long> participantIds,
        Long courtReservationId,
        boolean requiresPlayerPayment,
        String yapePhone,
        Double playerPaymentAmount,
        /** Nombre visible del organizador y de cada participante, indexado por id de usuario. */
        Map<Long, String> playerNames) {
}
