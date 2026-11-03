package com.partidoya.platform.subscriptions.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import com.partidoya.platform.subscriptions.models.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_customer_id", length = 120)
    private String providerCustomerId;

    @Column(name = "provider_card_id", length = 120)
    private String providerCardId;

    @Column(name = "provider_subscription_id", length = 120)
    private String providerSubscriptionId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "next_renewal_at")
    private LocalDateTime nextRenewalAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
}
