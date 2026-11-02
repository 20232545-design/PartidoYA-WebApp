package com.partidoya.platform.courts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CourtReportResource(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        long confirmedReservations,
        long canceledReservations,
        double occupancyPercentage,
        BigDecimal averageTicket,
        List<DailyIncomeResource> dailyIncome,
        CourtRankingResource topCourt,
        List<CourtRankingResource> ranking
) {
}
