package com.partidoya.platform.courts.models;

import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Getter
public class CourtAvailability {
    public static final LocalTime DAY_START = LocalTime.of(7, 0);
    public static final LocalTime DAY_END = LocalTime.of(23, 0);

    private CourtAvailabilityId id;
    private CourtId courtId;
    private LocalDate date;
    private boolean allDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private CourtAvailabilityType type;
    private String reason;
    private LocalDateTime createdAt;

    protected CourtAvailability() {
    }

    public CourtAvailability(CourtId courtId, LocalDate date, boolean allDay, LocalTime startTime, LocalTime endTime,
                             CourtAvailabilityType type, String reason) {
        this.courtId = Objects.requireNonNull(courtId, "courtId must not be null");
        this.createdAt = LocalDateTime.now();
        update(date, allDay, startTime, endTime, type, reason);
    }

    public CourtAvailability(CourtAvailabilityId id, CourtId courtId, LocalDate date, boolean allDay,
                             LocalTime startTime, LocalTime endTime, CourtAvailabilityType type, String reason,
                             LocalDateTime createdAt) {
        this(courtId, date, allDay, startTime, endTime, type, reason);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public void update(LocalDate date, boolean allDay, LocalTime startTime, LocalTime endTime,
                       CourtAvailabilityType type, String reason) {
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.allDay = allDay;
        this.startTime = allDay ? DAY_START : Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = allDay ? DAY_END : Objects.requireNonNull(endTime, "endTime must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (!this.endTime.isAfter(this.startTime)) throw new IllegalArgumentException("End time must be after start time");
        if (this.startTime.isBefore(DAY_START) || this.endTime.isAfter(DAY_END)) {
            throw new IllegalArgumentException("Availability must be between 07:00 and 23:00");
        }
        if (this.startTime.getMinute() != 0 || this.endTime.getMinute() != 0) {
            throw new IllegalArgumentException("Availability times must use one-hour intervals");
        }
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public boolean overlaps(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        return date.equals(otherDate) && startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public boolean contains(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        return date.equals(otherDate) && !startTime.isAfter(otherStart) && !endTime.isBefore(otherEnd);
    }
}
