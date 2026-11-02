package com.partidoya.platform.courts.application.internal.queryservices;

import com.partidoya.platform.courts.application.queryservices.CourtQueryService;
import com.partidoya.platform.courts.application.queryservices.CourtAvailabilityView;
import com.partidoya.platform.courts.application.queryservices.CourtReportView;
import com.partidoya.platform.courts.application.queryservices.ManagedReservationView;
import com.partidoya.platform.courts.domain.model.aggregates.Court;
import com.partidoya.platform.courts.domain.model.aggregates.CourtAvailability;
import com.partidoya.platform.courts.domain.model.queries.GetCourtByIdQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedAvailabilityQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedCourtsQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedCourtReportQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedReservationsQuery;
import com.partidoya.platform.courts.domain.model.queries.GetReservableSchedulesQuery;
import com.partidoya.platform.courts.domain.model.queries.SearchPublishedCourtsQuery;
import com.partidoya.platform.courts.domain.model.valueobjects.CourtAvailabilityType;
import com.partidoya.platform.courts.domain.repositories.CourtRepository;
import com.partidoya.platform.courts.domain.repositories.CourtAvailabilityRepository;
import com.partidoya.platform.courts.domain.repositories.ReservationRepository;
import com.partidoya.platform.courts.domain.model.valueobjects.ReservationStatus;
import com.partidoya.platform.iam.domain.repositories.UserRepository;
import com.partidoya.platform.iam.domain.services.PlanPolicy;
import com.partidoya.platform.shared.domain.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourtQueryServiceImpl implements CourtQueryService {
    private final CourtRepository courtRepository;
    private final CourtAvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CourtReportCalculator courtReportCalculator;

    public CourtQueryServiceImpl(CourtRepository courtRepository, CourtAvailabilityRepository availabilityRepository,
                                 ReservationRepository reservationRepository,
                                 UserRepository userRepository,
                                 CourtReportCalculator courtReportCalculator) {
        this.courtRepository = courtRepository;
        this.availabilityRepository = availabilityRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.courtReportCalculator = courtReportCalculator;
    }

    @Override
    public List<Court> handle(SearchPublishedCourtsQuery query) {
        return courtRepository.findPublished(query.sport(), query.district()).stream()
                .filter(court -> userRepository.findById(court.getOwnerId())
                        .map(PlanPolicy::canManageCourts)
                        .orElse(false))
                .toList();
    }

    @Override
    public Optional<Court> handle(GetCourtByIdQuery query) {
        return courtRepository.findById(query.courtId())
                .filter(Court::canReceiveReservations)
                .filter(court -> userRepository.findById(court.getOwnerId())
                        .map(PlanPolicy::canManageCourts)
                        .orElse(false));
    }

    @Override
    public List<Court> handle(GetManagedCourtsQuery query) {
        return courtRepository.findByOwnerId(query.ownerId());
    }

    @Override
    public List<ManagedReservationView> handle(GetManagedReservationsQuery query) {
        var courts = managedCourts(query.ownerId(), query.courtId());
        if (courts.isEmpty()) return List.of();

        var range = dateRange(query.from(), query.to());

        Map<Long, Court> courtsById = courts.stream()
                .collect(Collectors.toMap(court -> court.getId().value(), Function.identity()));

        return reservationRepository.findByCourtIds(courts.stream().map(Court::getId).toList(), range.from(), range.to())
                .stream()
                .map(reservation -> {
                    var court = courtsById.get(reservation.getCourtId().value());
                    var customerName = userRepository.findById(reservation.getUserId())
                            .map(user -> user.getFullName().value())
                            .orElse("Usuario #" + reservation.getUserId().value());
                    return new ManagedReservationView(
                            reservation.getId().value(),
                            reservation.getUserId().value(),
                            customerName,
                            reservation.getCourtId().value(),
                            court != null ? court.getName() : "Cancha #" + reservation.getCourtId().value(),
                            reservation.getDate(),
                            reservation.getStartTime(),
                            reservation.getEndTime(),
                            reservation.getPrice(),
                            reservation.getCurrency(),
                            reservation.getProviderReference(),
                            reservation.getStatus().name(),
                            reservation.getPaymentStatus().name(),
                            reservation.getCreatedAt());
                })
                .toList();
    }

    @Override
    public List<CourtAvailabilityView> handle(GetManagedAvailabilityQuery query) {
        var courts = managedCourts(query.ownerId(), query.courtId());
        if (courts.isEmpty()) return List.of();
        var range = dateRange(query.from(), query.to());
        Map<Long, Court> courtsById = courts.stream()
                .collect(Collectors.toMap(court -> court.getId().value(), Function.identity()));
        return availabilityRepository.findByCourtIds(courts.stream().map(Court::getId).toList(), range.from(), range.to())
                .stream()
                .map(availability -> toAvailabilityView(availability, courtsById.get(availability.getCourtId().value())))
                .toList();
    }

    @Override
    public List<String> handle(GetReservableSchedulesQuery query) {
        var court = courtRepository.findById(query.courtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court", query.courtId().value().toString()));
        var owner = userRepository.findById(court.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", court.getOwnerId().value().toString()));
        if (!PlanPolicy.canManageCourts(owner) || !court.canReceiveReservations()) {
            throw new ForbiddenActionException("Court is not available for reservations");
        }
        var date = query.date() == null ? LocalDate.now() : query.date();
        var availability = availabilityRepository.findByCourtIdAndDate(query.courtId(), date);
        var baseSchedules = new ArrayList<>(court.getSchedules());
        var explicitAvailable = availability.stream()
                .filter(item -> item.getType() == CourtAvailabilityType.AVAILABLE)
                .flatMap(item -> schedulesWithin(item.getStartTime(), item.getEndTime()).stream())
                .toList();
        if (!explicitAvailable.isEmpty()) {
            baseSchedules.addAll(explicitAvailable);
        }
        var reservations = reservationRepository.findByCourtIdAndDate(query.courtId(), date);
        return baseSchedules.stream()
                .distinct()
                .filter(schedule -> !isBlocked(schedule, date, availability))
                .filter(schedule -> !isReserved(schedule, date, reservations))
                .sorted()
                .toList();
    }

    @Override
    public CourtReportView handle(GetManagedCourtReportQuery query) {
        var courts = managedCourts(query.ownerId(), query.courtId());
        var range = dateRange(query.from(), query.to());
        if (courts.isEmpty()) {
            return courtReportCalculator.calculate(courts, List.of(), range.from(), range.to());
        }
        var reservations = reservationRepository.findByCourtIds(courts.stream().map(Court::getId).toList(),
                range.from(), range.to());
        return courtReportCalculator.calculate(courts, reservations, range.from(), range.to());
    }

    private List<Court> managedCourts(com.partidoya.platform.iam.domain.model.valueobjects.UserId ownerId,
                                      com.partidoya.platform.courts.domain.model.valueobjects.CourtId courtId) {
        var owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId.value().toString()));
        if (!PlanPolicy.canManageCourts(owner)) {
            throw new ForbiddenActionException("Only court administrators can manage court data");
        }
        var courts = courtRepository.findByOwnerId(ownerId);
        if (courtId == null) return courts;
        var filtered = courts.stream().filter(court -> court.getId().value().equals(courtId.value())).toList();
        if (filtered.isEmpty()) throw new ForbiddenActionException("You cannot access this court");
        return filtered;
    }

    private static CourtAvailabilityView toAvailabilityView(CourtAvailability availability, Court court) {
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

    private static DateRange dateRange(LocalDate from, LocalDate to) {
        var resolvedFrom = from == null ? LocalDate.now().minusMonths(1) : from;
        var resolvedTo = to == null ? LocalDate.now().plusMonths(3) : to;
        if (resolvedFrom.isAfter(resolvedTo)) throw new IllegalArgumentException("date range is invalid");
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private static boolean isBlocked(String schedule, LocalDate date, List<CourtAvailability> availability) {
        var range = scheduleRange(schedule);
        return availability.stream()
                .filter(item -> item.getType() == CourtAvailabilityType.BLOCKED)
                .anyMatch(item -> item.overlaps(date, range.start(), range.end()));
    }

    private static boolean isReserved(String schedule, LocalDate date,
                                      List<com.partidoya.platform.courts.domain.model.aggregates.Reservation> reservations) {
        var range = scheduleRange(schedule);
        return reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .anyMatch(reservation -> reservation.overlaps(date, range.start(), range.end()));
    }

    private static List<String> schedulesWithin(LocalTime start, LocalTime end) {
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

    private static ScheduleRange scheduleRange(String schedule) {
        var parts = schedule.split("-");
        return new ScheduleRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private record ScheduleRange(LocalTime start, LocalTime end) {
    }
}
