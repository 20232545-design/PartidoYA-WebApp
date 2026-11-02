package com.partidoya.platform.courts.application.internal.queryservices;

import com.partidoya.platform.courts.application.queryservices.CourtRankingView;
import com.partidoya.platform.courts.application.queryservices.CourtReportView;
import com.partidoya.platform.courts.application.queryservices.DailyIncomeView;
import com.partidoya.platform.courts.domain.model.aggregates.Court;
import com.partidoya.platform.courts.domain.model.aggregates.Reservation;
import com.partidoya.platform.courts.domain.model.valueobjects.PaymentStatus;
import com.partidoya.platform.courts.domain.model.valueobjects.ReservationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CourtReportCalculator {
    public CourtReportView calculate(
            List<Court> courts,
            List<Reservation> reservations,
            LocalDate from,
            LocalDate to) {
        if (courts.isEmpty()) {
            return new CourtReportView(from, to, BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO,
                    List.of(), null, List.of());
        }
        var confirmed = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED
                        && reservation.getPaymentStatus() == PaymentStatus.APPROVED)
                .toList();
        var canceled = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CANCELLED
                        || reservation.getPaymentStatus() == PaymentStatus.REJECTED
                        || reservation.getPaymentStatus() == PaymentStatus.REFUNDED)
                .count();
        var totalIncome = confirmed.stream()
                .map(reservation -> reservation.getPrice() == null ? BigDecimal.ZERO : reservation.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var averageTicket = confirmed.isEmpty()
                ? BigDecimal.ZERO
                : totalIncome.divide(BigDecimal.valueOf(confirmed.size()), 2, RoundingMode.HALF_UP);
        var possibleSlots = possibleSlots(courts, from, to);
        var occupancy = possibleSlots == 0 ? 0 : (confirmed.size() * 100.0) / possibleSlots;
        var dailyIncome = dailyIncome(confirmed, from, to);
        var ranking = ranking(courts, confirmed);
        return new CourtReportView(
                from,
                to,
                totalIncome,
                confirmed.size(),
                canceled,
                occupancy,
                averageTicket,
                dailyIncome,
                ranking.isEmpty() ? null : ranking.getFirst(),
                ranking);
    }

    private List<DailyIncomeView> dailyIncome(List<Reservation> confirmed, LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    var reservations = confirmed.stream()
                            .filter(reservation -> reservation.getDate().equals(date))
                            .toList();
                    var income = reservations.stream()
                            .map(reservation -> reservation.getPrice() == null ? BigDecimal.ZERO : reservation.getPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DailyIncomeView(date, income, reservations.size());
                })
                .toList();
    }

    private List<CourtRankingView> ranking(List<Court> courts, List<Reservation> confirmed) {
        Map<Long, Court> courtsById = courts.stream()
                .collect(Collectors.toMap(court -> court.getId().value(), Function.identity()));
        return confirmed.stream()
                .collect(Collectors.groupingBy(reservation -> reservation.getCourtId().value(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    var court = courtsById.get(entry.getKey());
                    var courtName = court == null ? "Cancha #" + entry.getKey() : court.getName();
                    var percentage = confirmed.isEmpty() ? 0 : (entry.getValue() * 100.0) / confirmed.size();
                    return new CourtRankingView(entry.getKey(), courtName, entry.getValue(), percentage);
                })
                .sorted(Comparator.comparingLong(CourtRankingView::reservations).reversed())
                .toList();
    }

    private long possibleSlots(List<Court> courts, LocalDate from, LocalDate to) {
        var days = ChronoUnit.DAYS.between(from, to) + 1;
        return courts.stream().mapToLong(court -> (long) court.getSchedules().size() * days).sum();
    }
}
