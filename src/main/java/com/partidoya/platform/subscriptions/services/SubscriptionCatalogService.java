package com.partidoya.platform.subscriptions.services;

import com.partidoya.platform.subscriptions.dto.SubscriptionPlanResource;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SubscriptionCatalogService {
    private final SubscriptionPlanRepository repository;
    private final String publicKey;
    private final String secretKey;
    private final Map<String, String> providerPlanIds;

    public SubscriptionCatalogService(
            SubscriptionPlanRepository repository,
            @Value("${culqi.public-key:}") String publicKey,
            @Value("${culqi.secret-key:}") String secretKey,
            @Value("${culqi.subscription-plan-ids.jugador-plus:}") String playerPlus,
            @Value("${culqi.subscription-plan-ids.cancha-emprendedor:}") String courtEntrepreneur,
            @Value("${culqi.subscription-plan-ids.cancha-business:}") String courtBusiness) {
        this.repository = repository;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
        this.providerPlanIds = Map.of(
                "JUGADOR_PLUS", playerPlus,
                "CANCHA_EMPRENDEDOR", courtEntrepreneur,
                "CANCHA_BUSINESS", courtBusiness);
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
        var planId = providerPlanIds.get(planCode);
        return notBlank(publicKey) && notBlank(secretKey) && notBlank(planId);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
