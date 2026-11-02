package com.partidoya.platform.matches.dto;

import java.util.List;

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
        Double playerPaymentAmount) {
}
