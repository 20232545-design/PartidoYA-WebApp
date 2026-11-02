package com.partidoya.platform.courts.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CourtReportView(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        long confirmedReservations,
        long canceledReservations,
        double occupancyPercentage,
        BigDecimal averageTicket,
        List<DailyIncomeView> dailyIncome,
        CourtRankingView topCourt,
        List<CourtRankingView> ranking
) {
}
