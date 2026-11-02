package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class MatchPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "court_reservation_id")
    private Long courtReservationId;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "requires_player_payment", nullable = false)
    private boolean requiresPlayerPayment;

    @Column(name = "yape_phone", length = 20)
    private String yapePhone;

    @Column(name = "player_payment_amount", precision = 10, scale = 2)
    private BigDecimal playerPaymentAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "match_participants", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "user_id")
    private List<Long> participantIds = new ArrayList<>();
}
