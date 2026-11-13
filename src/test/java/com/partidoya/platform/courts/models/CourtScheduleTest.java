package com.partidoya.platform.courts.models;

import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourtScheduleTest {
    /** Viernes. */
    private static final LocalDate FRIDAY = LocalDate.of(2027, 11, 12);
    /** Sábado (la fecha que falló en producción: 13/11/2027). */
    private static final LocalDate SATURDAY = LocalDate.of(2027, 11, 13);

    @Test
    void weeklyOpeningHoursAreSplitIntoSlotsOfTheConfiguredDuration() {
        var schedule = CourtSchedule.parse("SATURDAY|08:00|11:00|60").orElseThrow();

        assertThat(labels(schedule, SATURDAY)).containsExactly("08:00-09:00", "09:00-10:00", "10:00-11:00");
    }

    @Test
    void weeklyOpeningHoursOnlyApplyToTheirDay() {
        var schedule = CourtSchedule.parse("SATURDAY|08:00|22:00|60").orElseThrow();

        assertThat(schedule.slotsOn(FRIDAY)).isEmpty();
    }

    @Test
    void anIncompleteLastSlotIsDiscarded() {
        var schedule = CourtSchedule.parse("SATURDAY|08:00|10:30|90").orElseThrow();

        assertThat(labels(schedule, SATURDAY)).containsExactly("08:00-09:30");
    }

    @Test
    void slotsNeverWrapPastMidnight() {
        var schedule = CourtSchedule.parse("SATURDAY|22:00|23:59|60").orElseThrow();

        assertThat(labels(schedule, SATURDAY)).containsExactly("22:00-23:00");
    }

    @Test
    void legacyFixedSlotsApplyEveryDay() {
        var schedule = CourtSchedule.parse("18:00-19:00").orElseThrow();

        assertThat(labels(schedule, FRIDAY)).containsExactly("18:00-19:00");
        assertThat(labels(schedule, SATURDAY)).containsExactly("18:00-19:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "basura", "SOMEDAY|08:00|22:00|60", "MONDAY|22:00|08:00|60",
            "MONDAY|08:00|22:00|0", "MONDAY|8am|22:00|60", "19:00-18:00", "18:00"})
    void malformedEntriesAreIgnoredInsteadOfBreakingTheCourt(String value) {
        assertThat(CourtSchedule.parse(value)).isEmpty();
    }

    @Test
    void courtCombinesItsEntriesSortedAndWithoutDuplicates() {
        var court = court(List.of("SATURDAY|20:00|22:00|60", "18:00-19:00", "20:00-21:00", "MONDAY|08:00|09:00|60", "basura"));

        assertThat(court.slotsOn(SATURDAY)).containsExactly("18:00-19:00", "20:00-21:00", "21:00-22:00");
        assertThat(court.supportsSlot(SATURDAY, LocalTime.of(21, 0), LocalTime.of(22, 0))).isTrue();
        assertThat(court.supportsSlot(FRIDAY, LocalTime.of(21, 0), LocalTime.of(22, 0))).isFalse();
    }

    private static List<String> labels(CourtSchedule schedule, LocalDate date) {
        return schedule.slotsOn(date).stream().map(CourtSchedule.TimeSlot::label).toList();
    }

    private static Court court(List<String> schedules) {
        return new Court(new CourtId(1L), new UserId(1L), "Cancha Central", null, null, "Av. Principal 100",
                "Miraflores", null, null, new BigDecimal("50.00"), true, true, true, List.of("FUTBOL"),
                schedules, null, List.of(), List.of(), List.of());
    }
}
