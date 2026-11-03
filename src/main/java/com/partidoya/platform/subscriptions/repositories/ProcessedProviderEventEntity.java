package com.partidoya.platform.subscriptions.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_provider_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedProviderEventEntity extends AuditableAbstractPersistenceEntity {
    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 120)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
