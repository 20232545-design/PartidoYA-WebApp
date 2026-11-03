package com.partidoya.platform.subscriptions.services;

import com.partidoya.platform.profiles.repositories.ProfileRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.subscriptions.dto.CreateSubscriptionResource;
import com.partidoya.platform.subscriptions.dto.MySubscriptionResource;
import com.partidoya.platform.subscriptions.dto.SubscriptionPaymentResource;
import com.partidoya.platform.subscriptions.dto.SubscriptionResource;
import com.partidoya.platform.subscriptions.models.SubscriptionPaymentStatus;
import com.partidoya.platform.subscriptions.models.SubscriptionStatus;
import com.partidoya.platform.subscriptions.payments.SubscriptionCreationRequest;
import com.partidoya.platform.subscriptions.payments.SubscriptionPaymentGateway;
import com.partidoya.platform.subscriptions.payments.VerifiedSubscriptionEvent;
import com.partidoya.platform.subscriptions.repositories.ProcessedProviderEventEntity;
import com.partidoya.platform.subscriptions.repositories.ProcessedProviderEventRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPaymentEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPaymentRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionRepository;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Transactional
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final ProcessedProviderEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SubscriptionPaymentGateway paymentGateway;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            SubscriptionPaymentRepository paymentRepository,
            ProcessedProviderEventRepository eventRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            SubscriptionPaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.paymentGateway = paymentGateway;
    }

    public SubscriptionResource create(UserId userId, CreateSubscriptionResource resource) {
        validateCreate(resource);
        var existing = subscriptionRepository.findByIdempotencyKey(resource.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().getUserId().equals(userId.value())) {
                throw new ResourceConflictException("Subscription", "Idempotency key is already in use");
            }
            return toResource(existing.get());
        }
        var user = requireUser(userId);
        var plan = requirePaidPlan(resource.planCode());
        if (!plan.getRole().equals(user.getRole().name())) {
            throw new ForbiddenActionException("Selected plan is not available for your account type");
        }
        var active = subscriptionRepository.findByUserIdAndStatus(userId.value(), SubscriptionStatus.ACTIVE);
        if (!active.isEmpty()) {
            throw new ResourceConflictException("Subscription", "An active subscription already exists");
        }

        var subscription = new SubscriptionEntity();
        subscription.setUserId(userId.value());
        subscription.setPlanCode(plan.getCode());
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setProvider("CULQI");
        subscription.setIdempotencyKey(resource.idempotencyKey().trim());
        subscription = subscriptionRepository.save(subscription);

        var profile = profileRepository.findByUserId(userId).orElse(null);
        try {
            var provider = paymentGateway.createSubscription(new SubscriptionCreationRequest(
                    userId.value(),
                    user.getEmail().value(),
                    user.getFirstName(),
                    user.getLastName(),
                    profile == null || profile.getPhoneNumber() == null ? null : profile.getPhoneNumber().value(),
                    profile == null ? null : profile.getDistrict(),
                    plan.getCode(),
                    resource.tokenId().trim(),
                    resource.idempotencyKey().trim()));
            subscription.setProviderCustomerId(provider.customerId());
            subscription.setProviderCardId(provider.cardId());
            subscription.setProviderSubscriptionId(provider.subscriptionId());
        } catch (RuntimeException exception) {
            subscription.setStatus(SubscriptionStatus.FAILED);
        }
        return toResource(subscriptionRepository.save(subscription));
    }

    @Transactional(readOnly = true)
    public MySubscriptionResource findMine(UserId userId) {
        var user = requireUser(userId);
        var subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId.value())
                .map(this::toResource)
                .orElse(null);
        var payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId.value()).stream()
                .map(this::toPaymentResource)
                .toList();
        return new MySubscriptionResource(user.getPlan().name(), user.isLegacyPlanAccess(), subscription, payments);
    }

    public SubscriptionResource cancelMine(UserId userId) {
        var subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId.value())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", userId.value().toString()));
        if (subscription.getStatus() == SubscriptionStatus.CANCELED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED
                || subscription.getStatus() == SubscriptionStatus.FAILED) {
            return toResource(subscription);
        }
        if (subscription.getProviderSubscriptionId() != null) {
            paymentGateway.cancelSubscription(subscription.getProviderSubscriptionId());
        }
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        downgrade(requireUser(userId));
        return toResource(subscriptionRepository.save(subscription));
    }

    public void processCulqiEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Culqi event id is required");
        }
        var verified = paymentGateway.verifyEvent(eventId.trim());
        if (eventRepository.existsByProviderAndProviderEventId("CULQI", verified.eventId())) {
            return;
        }
        if (verified.subscriptionId() == null || verified.subscriptionId().isBlank()) {
            throw new IllegalArgumentException("Verified Culqi event has no subscription identifier");
        }
        var subscription = subscriptionRepository.findByProviderSubscriptionId(verified.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", verified.subscriptionId()));
        applyEvent(subscription, verified);
        saveProcessedEvent(verified);
    }

    private void applyEvent(SubscriptionEntity subscription, VerifiedSubscriptionEvent event) {
        switch (event.type()) {
            case "subscription.charge.succeeded" -> activate(subscription, event);
            case "subscription.charge.failed" -> failPayment(subscription, event, "Culqi rejected the subscription charge");
            case "subscription.creation.failed" -> {
                subscription.setStatus(SubscriptionStatus.FAILED);
                downgrade(requireUser(new UserId(subscription.getUserId())));
            }
            case "subscription.cancel.succeeded" -> {
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscription.setCanceledAt(event.occurredAt());
                downgrade(requireUser(new UserId(subscription.getUserId())));
            }
            case "subscription.trial.end", "subscription.creation.succeeded" -> {
            }
            default -> throw new IllegalArgumentException("Unsupported Culqi subscription event: " + event.type());
        }
        subscriptionRepository.save(subscription);
    }

    private void activate(SubscriptionEntity subscription, VerifiedSubscriptionEvent event) {
        if (paymentRepository.existsByProviderEventId(event.eventId())) return;
        var plan = requirePlan(subscription.getPlanCode());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(event.occurredAt());
        subscription.setCurrentPeriodEnd(event.occurredAt().plusMonths(1));
        subscription.setNextRenewalAt(event.occurredAt().plusMonths(1));
        var user = requireUser(new UserId(subscription.getUserId()));
        user.activateSubscriptionPlan(Plan.valueOf(plan.getCode()));
        userRepository.save(user);
        paymentRepository.save(payment(subscription, plan, event, SubscriptionPaymentStatus.SUCCEEDED, null));
    }

    private void failPayment(SubscriptionEntity subscription, VerifiedSubscriptionEvent event, String reason) {
        if (paymentRepository.existsByProviderEventId(event.eventId())) return;
        var plan = requirePlan(subscription.getPlanCode());
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        downgrade(requireUser(new UserId(subscription.getUserId())));
        paymentRepository.save(payment(subscription, plan, event, SubscriptionPaymentStatus.FAILED, reason));
    }

    private SubscriptionPaymentEntity payment(SubscriptionEntity subscription, SubscriptionPlanEntity plan,
                                              VerifiedSubscriptionEvent event, SubscriptionPaymentStatus status,
                                              String failureReason) {
        var payment = new SubscriptionPaymentEntity();
        payment.setSubscriptionId(subscription.getId());
        payment.setUserId(subscription.getUserId());
        payment.setPlanCode(plan.getCode());
        payment.setAmountCents(plan.getAmountCents());
        payment.setCurrency(plan.getCurrency());
        payment.setStatus(status);
        payment.setProviderTransactionId(event.transactionId());
        payment.setProviderEventId(event.eventId());
        payment.setPaidAt(status == SubscriptionPaymentStatus.SUCCEEDED ? event.occurredAt() : null);
        payment.setFailureReason(failureReason);
        return payment;
    }

    private void saveProcessedEvent(VerifiedSubscriptionEvent event) {
        var entity = new ProcessedProviderEventEntity();
        entity.setProvider("CULQI");
        entity.setProviderEventId(event.eventId());
        entity.setEventType(event.type());
        entity.setPayloadHash(sha256(event.rawPayload()));
        entity.setProcessedAt(LocalDateTime.now());
        eventRepository.save(entity);
    }

    private void downgrade(User user) {
        if (!user.isLegacyPlanAccess()) {
            user.downgradeAfterSubscription();
            userRepository.save(user);
        }
    }

    private User requireUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
    }

    private SubscriptionPlanEntity requirePaidPlan(String code) {
        var plan = requirePlan(code);
        if (!plan.isActive() || plan.getAmountCents() <= 0) {
            throw new IllegalArgumentException("Only active paid plans can create a subscription");
        }
        return plan;
    }

    private SubscriptionPlanEntity requirePlan(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Plan is required");
        return planRepository.findById(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan", code));
    }

    private void validateCreate(CreateSubscriptionResource resource) {
        if (resource == null || resource.tokenId() == null || resource.tokenId().isBlank()) {
            throw new IllegalArgumentException("Culqi card token is required");
        }
        if (!resource.termsAccepted()) {
            throw new IllegalArgumentException("Subscription terms must be accepted");
        }
        if (resource.idempotencyKey() == null || resource.idempotencyKey().isBlank()
                || resource.idempotencyKey().length() > 120) {
            throw new IllegalArgumentException("A valid idempotency key is required");
        }
    }

    private SubscriptionResource toResource(SubscriptionEntity entity) {
        return new SubscriptionResource(entity.getId(), entity.getPlanCode(), entity.getStatus().name(),
                entity.getCurrentPeriodStart(), entity.getCurrentPeriodEnd(), entity.getNextRenewalAt(), entity.getCanceledAt());
    }

    private SubscriptionPaymentResource toPaymentResource(SubscriptionPaymentEntity entity) {
        return new SubscriptionPaymentResource(entity.getId(), entity.getPlanCode(), entity.getAmountCents(),
                entity.getCurrency(), entity.getStatus().name(), entity.getPaidAt(), entity.getFailureReason());
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash provider event");
        }
    }
}
