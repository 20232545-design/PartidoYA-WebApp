package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.ManagedReservationView;
import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.Reservation;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtAvailabilityRepository;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.courts.payments.PaymentProcessor;
import com.partidoya.platform.courts.payments.PaymentRequest;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.users.services.PlanPolicy;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourtReservationService {
    private final CourtAvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final PaymentProcessor paymentProcessor;
    private final CourtAccessPolicy accessPolicy;

    public CourtReservationService(
            CourtAvailabilityRepository availabilityRepository,
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            PaymentProcessor paymentProcessor,
            CourtAccessPolicy accessPolicy) {
        this.availabilityRepository = availabilityRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.paymentProcessor = paymentProcessor;
        this.accessPolicy = accessPolicy;
    }

    public Reservation reserve(CourtReservationRequest request) {
        requirePlayer(request);
        var court = accessPolicy.requireReservableCourt(request.courtId());
        requireFutureSchedule(request);
        requireSupportedSchedule(court, request);
        requireUnblockedSchedule(request);
        requireNoReservationConflict(request);
        requireNewPayment(request);
        requireMatchingPaymentContext(request);
        var reservation = reservationRepository.save(new Reservation(
                request.userId(),
                request.courtId(),
                request.date(),
                request.startTime(),
                request.endTime(),
                court.getPricePerHour()));
        var payment = paymentProcessor.process(new PaymentRequest(
                reservation.getId().value(),
                reservation.getPrice(),
                reservation.getCurrency(),
                request.culqiToken(),
                request.payerEmail(),
                request.idempotencyKey(),
                "Reserva de cancha %s".formatted(court.getName())));
        if (!payment.approved()) {
            throw new IllegalStateException("Pago rechazado por Culqi.");
        }
        if (reservationRepository.existsByProviderReference(payment.providerReference())) {
            throw new ResourceConflictException("Payment", "provider operation already registered");
        }
        reservation.approvePayment(payment.providerReference(), request.idempotencyKey());
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<ManagedReservationView> listManaged(
            UserId ownerId,
            CourtId courtId,
            LocalDate from,
            LocalDate to) {
        var courts = accessPolicy.managedCourts(ownerId, courtId);
        if (courts.isEmpty()) return List.of();
        var range = DateRange.resolve(from, to);
        Map<Long, Court> courtsById = courts.stream()
                .collect(Collectors.toMap(court -> court.getId().value(), Function.identity()));
        return reservationRepository.findByCourtIds(
                        courts.stream().map(Court::getId).toList(),
                        range.from(),
                        range.to())
                .stream()
                .map(reservation -> toManagedView(reservation, courtsById))
                .toList();
    }

    private ManagedReservationView toManagedView(Reservation reservation, Map<Long, Court> courtsById) {
        var court = courtsById.get(reservation.getCourtId().value());
        var customerName = userRepository.findById(reservation.getUserId())
                .map(user -> user.getFullName().value())
                .orElse("Usuario #" + reservation.getUserId().value());
        return new ManagedReservationView(
                reservation.getId().value(),
                reservation.getUserId().value(),
                customerName,
                reservation.getCourtId().value(),
                court == null ? "Cancha #" + reservation.getCourtId().value() : court.getName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getCurrency(),
                reservation.getProviderReference(),
                reservation.getStatus().name(),
                reservation.getPaymentStatus().name(),
                reservation.getCreatedAt());
    }

    private void requirePlayer(CourtReservationRequest request) {
        var user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId().value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(user)) {
            throw new ForbiddenActionException("Only player accounts can reserve courts");
        }
    }

    private void requireFutureSchedule(CourtReservationRequest request) {
        if (LocalDateTime.of(request.date(), request.startTime()).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot reserve past schedules");
        }
    }

    private void requireSupportedSchedule(Court court, CourtReservationRequest request) {
        var availability = availabilityRepository.findByCourtIdAndDate(request.courtId(), request.date());
        var explicitlyAvailable = availability.stream()
                .filter(item -> item.getType() == CourtAvailabilityType.AVAILABLE)
                .anyMatch(item -> item.contains(request.date(), request.startTime(), request.endTime()));
        if (!court.supportsSlot(request.date(), request.startTime(), request.endTime()) && !explicitlyAvailable) {
            throw new IllegalStateException("Selected schedule is not available for this court");
        }
    }

    private void requireUnblockedSchedule(CourtReservationRequest request) {
        var blocked = availabilityRepository.findByCourtIdAndDate(request.courtId(), request.date()).stream()
                .filter(item -> item.getType() == CourtAvailabilityType.BLOCKED)
                .anyMatch(item -> item.overlaps(request.date(), request.startTime(), request.endTime()));
        if (blocked) {
            throw new ResourceConflictException("Availability", "schedule is blocked");
        }
    }

    private void requireNoReservationConflict(CourtReservationRequest request) {
        if (reservationRepository.existsOverlapping(
                request.courtId(),
                request.date(),
                request.startTime(),
                request.endTime())) {
            throw new ResourceConflictException("Reservation", "schedule already reserved");
        }
    }

    private void requireNewPayment(CourtReservationRequest request) {
        if (reservationRepository.existsByPaymentIdempotencyKey(request.idempotencyKey())) {
            throw new ResourceConflictException("Payment", "payment request already processed");
        }
    }

    private void requireMatchingPaymentContext(CourtReservationRequest request) {
        var expectedPrefix = "reservation-%d-%d-%s-%s-".formatted(
                request.courtId().value(),
                request.userId().value(),
                request.date(),
                request.startTime());
        if (request.idempotencyKey() == null || !request.idempotencyKey().startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Payment context does not match the selected reservation");
        }
    }
}
