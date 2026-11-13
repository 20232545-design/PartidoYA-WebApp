package com.partidoya.platform.notifications.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "grouped_count", nullable = false)
    private int groupedCount;
}
