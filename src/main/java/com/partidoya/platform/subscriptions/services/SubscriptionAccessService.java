package com.partidoya.platform.subscriptions.services;

import com.partidoya.platform.subscriptions.models.SubscriptionStatus;
import com.partidoya.platform.subscriptions.repositories.SubscriptionRepository;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class SubscriptionAccessService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionAccessService(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    public User synchronize(User user) {
        var active = subscriptionRepository.findByUserIdAndStatus(user.getId().value(), SubscriptionStatus.ACTIVE);
        var expired = active.stream()
                .filter(item -> "ONE_TIME".equals(item.getPaymentMode()))
                .filter(item -> item.getCurrentPeriodEnd() != null && item.getCurrentPeriodEnd().isBefore(LocalDateTime.now()))
                .toList();
        expired.forEach(item -> {
            item.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(item);
        });
        if (!expired.isEmpty() && active.size() == expired.size() && !user.isLegacyPlanAccess()) {
            user.downgradeAfterSubscription();
            return userRepository.save(user);
        }
        return user;
    }
}
