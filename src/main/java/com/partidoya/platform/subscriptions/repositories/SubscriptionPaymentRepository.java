package com.partidoya.platform.subscriptions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, Long> {
    boolean existsByProviderEventId(String providerEventId);
    List<SubscriptionPaymentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
