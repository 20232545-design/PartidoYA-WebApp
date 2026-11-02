package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "court_availability")
@Getter
@Setter
@NoArgsConstructor
public class CourtAvailabilityPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "court_id", nullable = false)
    private Long courtId;

    @Column(name = "availability_date", nullable = false)
    private LocalDate date;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 200)
    private String reason;

    @Column(name = "availability_created_at", nullable = false)
    private LocalDateTime availabilityCreatedAt;
}
