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
import com.partidoya.platform.subscriptions.payments.ChargeRequest;
import com.partidoya.platform.subscriptions.payments.ChargeResult;
import com.partidoya.platform.subscriptions.payments.ChargeRejectedException;
import com.partidoya.platform.subscriptions.payments.OneTimePaymentGateway;
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
    private final OneTimePaymentGateway oneTimePaymentGateway;
    private final SubscriptionAccessService subscriptionAccessService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            SubscriptionPaymentRepository paymentRepository,
            ProcessedProviderEventRepository eventRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            SubscriptionPaymentGateway paymentGateway,
            OneTimePaymentGateway oneTimePaymentGateway,
            SubscriptionAccessService subscriptionAccessService) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.paymentGateway = paymentGateway;
        this.oneTimePaymentGateway = oneTimePaymentGateway;
        this.subscriptionAccessService = subscriptionAccessService;
    }

    public SubscriptionResource create(UserId userId, CreateSubscriptionResource resource) {
        validateCreate(resource);
        var normalizedKey = resource.idempotencyKey().trim();
        var existing = subscriptionRepository.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            if (!existing.get().getUserId().equals(userId.value())) {
                throw new ResourceConflictException("Subscription", "Idempotency key is already in use");
            }
            if (!existing.get().getPlanCode().equals(resource.planCode().trim().toUpperCase())) {
                throw new ResourceConflictException("Subscription", "Idempotency key belongs to another plan");
            }
            if (existing.get().getStatus() == SubscriptionStatus.ACTIVE) {
                return toResource(existing.get());
            }
        }
        var user = subscriptionAccessService.synchronize(requireUser(userId));
        var plan = requirePaidPlan(resource.planCode());
        if (!plan.getRole().equals(user.getRole().name())) {
            throw new ForbiddenActionException("Selected plan is not available for your account type");
        }
        var active = subscriptionRepository.findByUserIdAndStatus(userId.value(), SubscriptionStatus.ACTIVE);
        if (active.stream().anyMatch(item -> !"ONE_TIME".equals(item.getPaymentMode()))) {
            throw new ResourceConflictException("Subscription", "Cancela primero tu suscripción recurrente anterior para evitar cargos duplicados");
        }

        var subscription = existing.orElseGet(SubscriptionEntity::new);
        subscription.setUserId(userId.value());
        subscription.setPlanCode(plan.getCode());
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setProvider("CULQI_CHARGE");
        subscription.setPaymentMode("ONE_TIME");
        subscription.setIdempotencyKey(normalizedKey);
        subscription = subscriptionRepository.save(subscription);

        try {
            var charge = oneTimePaymentGateway.charge(new ChargeRequest(
                    userId.value(),
                    user.getEmail().value(),
                    plan.getCode(),
                    plan.getAmountCents(),
                    plan.getCurrency(),
                    resource.tokenId().trim(),
                    normalizedKey,
                    "PartidoYA - " + plan.getDisplayName()));
            verifyCharge(charge, plan);
            verifyChargeOwnership(charge, userId, plan.getCode(), normalizedKey);
            if (!paymentRepository.existsByProviderTransactionId(charge.chargeId())) {
                activateOneTime(subscription, plan, user, active, charge, null);
            }
        } catch (ChargeRejectedException exception) {
            subscription.setStatus(SubscriptionStatus.FAILED);
            paymentRepository.save(oneTimePayment(subscription, plan, null, SubscriptionPaymentStatus.FAILED,
                    safeFailureMessage(exception)));
        } catch (RuntimeException exception) {
            subscription.setStatus(SubscriptionStatus.PENDING);
            paymentRepository.save(oneTimePayment(subscription, plan, null, SubscriptionPaymentStatus.PENDING,
                    safeFailureMessage(exception)));
        }
        return toResource(subscriptionRepository.save(subscription));
    }

    public MySubscriptionResource findMine(UserId userId) {
        var user = subscriptionAccessService.synchronize(requireUser(userId));
        var entity = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId.value()).orElse(null);
        expireOneTimeIfNeeded(entity, user);
        var subscription = entity == null ? null : toResource(entity);
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
        if ("ONE_TIME".equals(subscription.getPaymentMode())) {
            throw new ResourceConflictException("Subscription", "Los pagos únicos no tienen renovación automática que cancelar");
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
        if ("charge.creation.succeeded".equals(verified.type())) {
            reconcileOneTimeCharge(verified);
            saveProcessedEvent(verified);
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

    private void activateOneTime(SubscriptionEntity subscription, SubscriptionPlanEntity plan, User user,
                                 java.util.List<SubscriptionEntity> active, ChargeResult charge, String providerEventId) {
        var periodStart = charge.occurredAt();
        var periodBase = active.stream()
                .filter(item -> "ONE_TIME".equals(item.getPaymentMode()))
                .map(SubscriptionEntity::getCurrentPeriodEnd)
                .filter(java.util.Objects::nonNull)
                .filter(end -> end.isAfter(periodStart))
                .max(LocalDateTime::compareTo)
                .orElse(periodStart);
        active.stream()
                .filter(item -> "ONE_TIME".equals(item.getPaymentMode()))
                .forEach(item -> {
                    item.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(item);
                });
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodBase.plusDays(30));
        subscription.setNextRenewalAt(null);
        user.activateSubscriptionPlan(Plan.valueOf(plan.getCode()));
        userRepository.save(user);
        var payment = oneTimePayment(subscription, plan, charge, SubscriptionPaymentStatus.SUCCEEDED, null);
        payment.setProviderEventId(providerEventId);
        paymentRepository.save(payment);
    }

    private void reconcileOneTimeCharge(VerifiedSubscriptionEvent event) {
        var charge = oneTimePaymentGateway.findCharge(event.transactionId());
        if (charge.idempotencyKey() == null || charge.planCode() == null || charge.userId() == null) {
            throw new IllegalArgumentException("El cargo no contiene los metadatos de conciliación de PartidoYA");
        }
        var subscription = subscriptionRepository.findByIdempotencyKey(charge.idempotencyKey())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", charge.idempotencyKey()));
        if (!subscription.getUserId().equals(charge.userId()) || !subscription.getPlanCode().equals(charge.planCode())) {
            throw new ForbiddenActionException("Los datos del cargo no coinciden con la suscripción");
        }
        var plan = requirePaidPlan(subscription.getPlanCode());
        verifyCharge(charge, plan);
        if (paymentRepository.existsByProviderTransactionId(charge.chargeId())) return;
        var user = requireUser(new UserId(subscription.getUserId()));
        var active = subscriptionRepository.findByUserIdAndStatus(subscription.getUserId(), SubscriptionStatus.ACTIVE);
        activateOneTime(subscription, plan, user, active, charge, event.eventId());
        subscriptionRepository.save(subscription);
    }

    private void verifyCharge(ChargeResult charge, SubscriptionPlanEntity plan) {
        if (charge == null
                || charge.chargeId() == null || charge.chargeId().isBlank()
                || charge.amountCents() != plan.getAmountCents()
                || !plan.getCurrency().equalsIgnoreCase(charge.currency())
                || !"Exitosa".equalsIgnoreCase(charge.state())
                || !"venta_exitosa".equalsIgnoreCase(charge.responseCode())) {
            throw new IllegalStateException("Culqi no confirmó el cargo con el importe y moneda esperados");
        }
    }

    private void verifyChargeOwnership(ChargeResult charge, UserId userId, String planCode, String idempotencyKey) {
        if (!userId.value().equals(charge.userId())
                || !planCode.equals(charge.planCode())
                || !idempotencyKey.equals(charge.idempotencyKey())) {
            throw new ForbiddenActionException("Los metadatos del cargo no coinciden con la operación solicitada");
        }
    }

    private SubscriptionPaymentEntity oneTimePayment(SubscriptionEntity subscription, SubscriptionPlanEntity plan,
                                                     ChargeResult charge, SubscriptionPaymentStatus status,
                                                     String failureReason) {
        var payment = new SubscriptionPaymentEntity();
        payment.setSubscriptionId(subscription.getId());
        payment.setUserId(subscription.getUserId());
        payment.setPlanCode(plan.getCode());
        payment.setAmountCents(plan.getAmountCents());
        payment.setCurrency(plan.getCurrency());
        payment.setStatus(status);
        payment.setProviderTransactionId(charge == null ? null : charge.chargeId());
        payment.setPaidAt(status == SubscriptionPaymentStatus.SUCCEEDED && charge != null ? charge.occurredAt() : null);
        payment.setFailureReason(failureReason);
        return payment;
    }

    private void expireOneTimeIfNeeded(SubscriptionEntity subscription, User user) {
        if (subscription == null || !"ONE_TIME".equals(subscription.getPaymentMode())
                || subscription.getStatus() != SubscriptionStatus.ACTIVE
                || subscription.getCurrentPeriodEnd() == null
                || !subscription.getCurrentPeriodEnd().isBefore(LocalDateTime.now())) {
            return;
        }
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
        downgrade(user);
    }

    private static String safeFailureMessage(RuntimeException exception) {
        var message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "El pago no pudo completarse"
                : exception.getMessage();
        return message.length() > 300 ? message.substring(0, 300) : message;
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
                entity.getPaymentMode(), entity.getCurrentPeriodStart(), entity.getCurrentPeriodEnd(),
                entity.getNextRenewalAt(), entity.getCanceledAt());
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
