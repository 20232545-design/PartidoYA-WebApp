package com.partidoya.platform.subscriptions.services;

import com.partidoya.platform.subscriptions.dto.SubscriptionPlanResource;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionCatalogService {
    private final SubscriptionPlanRepository repository;
    private final String publicKey;
    private final String secretKey;

    public SubscriptionCatalogService(
            SubscriptionPlanRepository repository,
            @Value("${culqi.public-key:}") String publicKey,
            @Value("${culqi.secret-key:}") String secretKey) {
        this.repository = repository;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResource> findActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(plan -> new SubscriptionPlanResource(
                        plan.getCode(),
                        plan.getDisplayName(),
                        plan.getDescription(),
                        plan.getRole(),
                        plan.getAmountCents(),
                        plan.getCurrency(),
                        plan.getIntervalUnit(),
                        plan.getAmountCents() > 0,
                        plan.getAmountCents() == 0 || providerAvailable(plan.getCode())))
                .toList();
    }

    private boolean providerAvailable(String planCode) {
        return notBlank(publicKey) && notBlank(secretKey);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
