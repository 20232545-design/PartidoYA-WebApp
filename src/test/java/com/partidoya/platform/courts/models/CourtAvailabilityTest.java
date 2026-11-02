package com.partidoya.platform.courts.models;

import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourtAvailabilityTest {
    @Test
    void createsAllDayRecordUsingConfiguredCourtDay() {
        var availability = new CourtAvailability(new CourtId(1L), LocalDate.now(), true,
                null, null, CourtAvailabilityType.BLOCKED, "Mantenimiento");

        assertThat(availability.getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(availability.getEndTime()).isEqualTo(LocalTime.of(23, 0));
        assertThat(availability.overlaps(LocalDate.now(), LocalTime.of(12, 0), LocalTime.of(13, 0))).isTrue();
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        assertThatThrownBy(() -> new CourtAvailability(new CourtId(1L), LocalDate.now(), false,
                LocalTime.of(11, 0), LocalTime.of(10, 0), CourtAvailabilityType.AVAILABLE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTimesOutsideConfiguredDay() {
        assertThatThrownBy(() -> new CourtAvailability(new CourtId(1L), LocalDate.now(), false,
                LocalTime.of(6, 0), LocalTime.of(8, 0), CourtAvailabilityType.BLOCKED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
