package com.partidoya.platform.subscriptions.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, String> {
    List<SubscriptionPlanEntity> findByActiveTrueOrderByDisplayOrderAsc();
}
