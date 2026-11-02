package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.PaymentStatus;
import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.models.ReservationStatus;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourtReportServiceTest {
    private final CourtReportService reportService = new CourtReportService(null, null);

    @Test
    void calculatesIncomeOccupancyAndRankingFromApprovedReservations() {
        var date = LocalDate.of(2026, 11, 2);
        var court = court(10L, List.of("18:00-19:00", "19:00-20:00"));
        var confirmed = reservation(100L, 10L, date, new BigDecimal("50.00"),
                ReservationStatus.CONFIRMED, PaymentStatus.APPROVED);
        var canceled = reservation(101L, 10L, date, new BigDecimal("60.00"),
                ReservationStatus.CANCELLED, PaymentStatus.REFUNDED);

        var report = reportService.calculate(List.of(court), List.of(confirmed, canceled), date, date);

        assertThat(report.totalIncome()).isEqualByComparingTo("50.00");
        assertThat(report.confirmedReservations()).isEqualTo(1);
        assertThat(report.canceledReservations()).isEqualTo(1);
        assertThat(report.occupancyPercentage()).isEqualTo(50.0);
        assertThat(report.averageTicket()).isEqualByComparingTo("50.00");
        assertThat(report.dailyIncome()).singleElement().satisfies(day -> {
            assertThat(day.income()).isEqualByComparingTo("50.00");
            assertThat(day.reservations()).isEqualTo(1);
        });
        assertThat(report.topCourt().courtId()).isEqualTo(10L);
        assertThat(report.topCourt().reservations()).isEqualTo(1L);
    }

    @Test
    void returnsAnEmptyReportWhenOwnerHasNoCourts() {
        var from = LocalDate.of(2026, 11, 1);
        var to = LocalDate.of(2026, 11, 30);

        var report = reportService.calculate(List.of(), List.of(), from, to);

        assertThat(report.from()).isEqualTo(from);
        assertThat(report.to()).isEqualTo(to);
        assertThat(report.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.ranking()).isEmpty();
        assertThat(report.topCourt()).isNull();
    }

    private Court court(Long id, List<String> schedules) {
        return new Court(
                new CourtId(id),
                new UserId(1L),
                "Cancha Central",
                null,
                null,
                "Av. Principal 100",
                "Miraflores",
                null,
                null,
                new BigDecimal("50.00"),
                true,
                true,
                true,
                List.of("FUTBOL"),
                schedules,
                null,
                List.of(),
                List.of(),
                List.of());
    }

    private Reservation reservation(
            Long id,
            Long courtId,
            LocalDate date,
            BigDecimal price,
            ReservationStatus status,
            PaymentStatus paymentStatus) {
        return new Reservation(
                new ReservationId(id),
                new UserId(2L),
                new CourtId(courtId),
                date,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                price,
                "PEN",
                null,
                null,
                status,
                paymentStatus,
                LocalDateTime.of(date, LocalTime.of(12, 0)));
    }
}
