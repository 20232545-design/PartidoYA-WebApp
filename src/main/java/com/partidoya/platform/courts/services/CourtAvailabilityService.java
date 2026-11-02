package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.CourtAvailabilityView;
import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtAvailability;
import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.ReservationStatus;
import com.partidoya.platform.courts.repositories.CourtAvailabilityRepository;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import com.partidoya.platform.shared.domain.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourtAvailabilityService {
    private final CourtAvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;
    private final CourtAccessPolicy accessPolicy;

    public CourtAvailabilityService(
            CourtAvailabilityRepository availabilityRepository,
            ReservationRepository reservationRepository,
            CourtAccessPolicy accessPolicy) {
        this.availabilityRepository = availabilityRepository;
        this.reservationRepository = reservationRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<CourtAvailabilityView> listManaged(
            UserId ownerId,
            CourtId courtId,
            LocalDate from,
            LocalDate to) {
        var courts = accessPolicy.managedCourts(ownerId, courtId);
        if (courts.isEmpty()) return List.of();
        var range = DateRange.resolve(from, to);
        Map<Long, Court> courtsById = courts.stream()
                .collect(Collectors.toMap(court -> court.getId().value(), Function.identity()));
        return availabilityRepository.findByCourtIds(
                        courts.stream().map(Court::getId).toList(),
                        range.from(),
                        range.to())
                .stream()
                .map(availability -> toView(availability, courtsById.get(availability.getCourtId().value())))
                .toList();
    }

    public CourtAvailability create(UserId requesterId, CourtAvailabilityInput input) {
        accessPolicy.requireOwnedCourt(input.courtId(), requesterId);
        var availability = new CourtAvailability(
                input.courtId(),
                input.date(),
                input.allDay(),
                input.startTime(),
                input.endTime(),
                input.type(),
                input.reason());
        requireNoOverlap(availability, null);
        return availabilityRepository.save(availability);
    }

    public CourtAvailability update(
            UserId requesterId,
            CourtAvailabilityId availabilityId,
            CourtAvailabilityInput input) {
        var availability = requireAvailability(availabilityId);
        accessPolicy.requireOwnedCourt(input.courtId(), requesterId);
        if (!availability.getCourtId().value().equals(input.courtId().value())) {
            throw new IllegalArgumentException("Availability cannot be moved to another court");
        }
        availability.update(
                input.date(),
                input.allDay(),
                input.startTime(),
                input.endTime(),
                input.type(),
                input.reason());
        requireNoOverlap(availability, availabilityId);
        return availabilityRepository.save(availability);
    }

    public void delete(UserId requesterId, CourtAvailabilityId availabilityId) {
        var availability = requireAvailability(availabilityId);
        accessPolicy.requireOwnedCourt(availability.getCourtId(), requesterId);
        availabilityRepository.delete(availability);
    }

    @Transactional(readOnly = true)
    public List<String> findReservableSchedules(CourtId courtId, LocalDate requestedDate) {
        var court = accessPolicy.requireReservableCourt(courtId);
        var date = requestedDate == null ? LocalDate.now() : requestedDate;
        var availability = availabilityRepository.findByCourtIdAndDate(courtId, date);
        var schedules = new ArrayList<>(court.getSchedules());
        availability.stream()
                .filter(item -> item.getType() == CourtAvailabilityType.AVAILABLE)
                .flatMap(item -> schedulesWithin(item.getStartTime(), item.getEndTime()).stream())
                .forEach(schedules::add);
        var reservations = reservationRepository.findByCourtIdAndDate(courtId, date);
        return schedules.stream()
                .distinct()
                .filter(schedule -> !isBlocked(schedule, date, availability))
                .filter(schedule -> !isReserved(schedule, date, reservations))
                .sorted()
                .toList();
    }

    public CourtAvailabilityView findManagedView(
            UserId ownerId,
            CourtAvailability availability) {
        return listManaged(
                ownerId,
                availability.getCourtId(),
                availability.getDate(),
                availability.getDate())
                .stream()
                .filter(item -> item.id().equals(availability.getId().value()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CourtAvailability",
                        availability.getId().value().toString()));
    }

    private CourtAvailability requireAvailability(CourtAvailabilityId availabilityId) {
        return availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CourtAvailability",
                        availabilityId.value().toString()));
    }

    private void requireNoOverlap(CourtAvailability availability, CourtAvailabilityId excludedId) {
        if (availabilityRepository.existsOverlapping(
                availability.getCourtId(),
                availability.getDate(),
                availability.getStartTime(),
                availability.getEndTime(),
                excludedId)) {
            throw new ResourceConflictException("CourtAvailability", "schedule overlaps an existing record");
        }
    }

    private CourtAvailabilityView toView(CourtAvailability availability, Court court) {
        return new CourtAvailabilityView(
                availability.getId().value(),
                availability.getCourtId().value(),
                court == null ? "Cancha #" + availability.getCourtId().value() : court.getName(),
                availability.getDate(),
                availability.isAllDay(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getType().name(),
                availability.getReason(),
                availability.getCreatedAt());
    }

    private boolean isBlocked(String schedule, LocalDate date, List<CourtAvailability> availability) {
        var range = ScheduleRange.parse(schedule);
        return availability.stream()
                .filter(item -> item.getType() == CourtAvailabilityType.BLOCKED)
                .anyMatch(item -> item.overlaps(date, range.start(), range.end()));
    }

    private boolean isReserved(String schedule, LocalDate date, List<Reservation> reservations) {
        var range = ScheduleRange.parse(schedule);
        return reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .anyMatch(reservation -> reservation.overlaps(date, range.start(), range.end()));
    }

    private List<String> schedulesWithin(LocalTime start, LocalTime end) {
        var slots = new ArrayList<String>();
        var cursor = start;
        while (cursor.isBefore(end)) {
            var next = cursor.plusHours(1);
            if (next.isAfter(end)) break;
            slots.add("%s-%s".formatted(cursor, next));
            cursor = next;
        }
        return slots;
    }

    private record ScheduleRange(LocalTime start, LocalTime end) {
        private static ScheduleRange parse(String schedule) {
            var parts = schedule.split("-");
            return new ScheduleRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
        }
    }
}
