package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "court_reservations")
@Getter
@Setter
@NoArgsConstructor
public class ReservationPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "court_id", nullable = false)
    private Long courtId;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_reference", unique = true)
    private String providerReference;

    @Column(name = "payment_idempotency_key", unique = true)
    private String paymentIdempotencyKey;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(name = "reservation_created_at", nullable = false)
    private LocalDateTime reservationCreatedAt;
}
