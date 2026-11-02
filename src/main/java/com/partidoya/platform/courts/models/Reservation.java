package com.partidoya.platform.courts.models;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.models.PaymentStatus;
import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.models.ReservationStatus;
import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Getter
public class Reservation {
    private ReservationId id;
    private UserId userId;
    private CourtId courtId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private String currency;
    private String providerReference;
    private String paymentIdempotencyKey;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(UserId userId, CourtId courtId, LocalDate date, LocalTime startTime, LocalTime endTime,
                       BigDecimal price) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.courtId = Objects.requireNonNull(courtId, "courtId must not be null");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("end time must be after start time");
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.currency = "PEN";
        this.status = ReservationStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Reservation(ReservationId id, UserId userId, CourtId courtId, LocalDate date, LocalTime startTime,
                       LocalTime endTime, BigDecimal price, String currency, String providerReference,
                       String paymentIdempotencyKey, ReservationStatus status, PaymentStatus paymentStatus,
                       LocalDateTime createdAt) {
        this(userId, courtId, date, startTime, endTime, price);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.currency = currency == null || currency.isBlank() ? "PEN" : currency;
        this.providerReference = providerReference;
        this.paymentIdempotencyKey = paymentIdempotencyKey;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean overlaps(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        return date.equals(otherDate) && startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    public void approvePayment(String providerReference, String paymentIdempotencyKey) {
        if (this.paymentStatus == PaymentStatus.APPROVED) {
            throw new IllegalStateException("payment already approved");
        }
        this.providerReference = Objects.requireNonNull(providerReference, "providerReference must not be null");
        this.paymentIdempotencyKey = paymentIdempotencyKey;
        this.paymentStatus = PaymentStatus.APPROVED;
        this.status = ReservationStatus.CONFIRMED;
    }

    public boolean isConfirmedFor(UserId userId) {
        return this.userId.value().equals(userId.value()) && this.status == ReservationStatus.CONFIRMED
                && this.paymentStatus == PaymentStatus.APPROVED;
    }
}
