package com.partidoya.platform.subscriptions.repositories;

import com.partidoya.platform.subscriptions.models.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByIdempotencyKey(String idempotencyKey);
    Optional<SubscriptionEntity> findByProviderSubscriptionId(String providerSubscriptionId);
    Optional<SubscriptionEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    List<SubscriptionEntity> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
