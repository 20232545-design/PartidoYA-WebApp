package com.partidoya.platform.subscriptions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedProviderEventRepository extends JpaRepository<ProcessedProviderEventEntity, Long> {
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
