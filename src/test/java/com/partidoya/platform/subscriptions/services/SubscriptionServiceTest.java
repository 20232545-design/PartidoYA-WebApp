package com.partidoya.platform.subscriptions.services;

import com.partidoya.platform.profiles.repositories.ProfileRepository;
import com.partidoya.platform.subscriptions.dto.CreateSubscriptionResource;
import com.partidoya.platform.subscriptions.models.SubscriptionPaymentStatus;
import com.partidoya.platform.subscriptions.models.SubscriptionStatus;
import com.partidoya.platform.subscriptions.payments.ProviderSubscription;
import com.partidoya.platform.subscriptions.payments.SubscriptionPaymentGateway;
import com.partidoya.platform.subscriptions.payments.VerifiedSubscriptionEvent;
import com.partidoya.platform.subscriptions.repositories.ProcessedProviderEventRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPaymentEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPaymentRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanEntity;
import com.partidoya.platform.subscriptions.repositories.SubscriptionPlanRepository;
import com.partidoya.platform.subscriptions.repositories.SubscriptionRepository;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionPlanRepository planRepository;
    private SubscriptionPaymentRepository paymentRepository;
    private ProcessedProviderEventRepository eventRepository;
    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private SubscriptionPaymentGateway gateway;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        planRepository = mock(SubscriptionPlanRepository.class);
        paymentRepository = mock(SubscriptionPaymentRepository.class);
        eventRepository = mock(ProcessedProviderEventRepository.class);
        userRepository = mock(UserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        gateway = mock(SubscriptionPaymentGateway.class);
        service = new SubscriptionService(subscriptionRepository, planRepository, paymentRepository, eventRepository,
                userRepository, profileRepository, gateway);
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> {
            var subscription = invocation.<SubscriptionEntity>getArgument(0);
            if (subscription.getId() == null) subscription.setId(50L);
            return subscription;
        });
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void paidPlanRemainsPendingUntilVerifiedCharge() {
        var user = player(Plan.JUGADOR_BASICO, false);
        var plan = plan("JUGADOR_PLUS", "JUGADOR", 1490);
        when(subscriptionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserIdAndStatus(7L, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(userRepository.findById(new UserId(7L))).thenReturn(Optional.of(user));
        when(planRepository.findById("JUGADOR_PLUS")).thenReturn(Optional.of(plan));
        when(profileRepository.findByUserId(new UserId(7L))).thenReturn(Optional.empty());
        when(gateway.createSubscription(any())).thenReturn(new ProviderSubscription("cus-1", "crd-1", "sub-1"));

        var result = service.create(new UserId(7L), new CreateSubscriptionResource("JUGADOR_PLUS", "tkn-1", true, "key-1"));

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_BASICO);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifiedChargeActivatesPlanAndRecordsPayment() {
        var user = player(Plan.JUGADOR_BASICO, false);
        var subscription = subscription("sub-1", SubscriptionStatus.PENDING);
        var event = new VerifiedSubscriptionEvent("evt-1", "subscription.charge.succeeded", "sub-1", "chr-1",
                LocalDateTime.of(2026, 11, 2, 12, 0), "{\"id\":\"evt-1\"}");
        when(gateway.verifyEvent("evt-1")).thenReturn(event);
        when(eventRepository.existsByProviderAndProviderEventId("CULQI", "evt-1")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub-1")).thenReturn(Optional.of(subscription));
        when(planRepository.findById("JUGADOR_PLUS")).thenReturn(Optional.of(plan("JUGADOR_PLUS", "JUGADOR", 1490)));
        when(paymentRepository.existsByProviderEventId("evt-1")).thenReturn(false);
        when(userRepository.findById(new UserId(7L))).thenReturn(Optional.of(user));

        service.processCulqiEvent("evt-1");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_PLUS);
        var paymentCaptor = ArgumentCaptor.forClass(SubscriptionPaymentEntity.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(SubscriptionPaymentStatus.SUCCEEDED);
        assertThat(paymentCaptor.getValue().getAmountCents()).isEqualTo(1490);
        verify(eventRepository).save(any());
    }

    @Test
    void repeatedWebhookIsIdempotent() {
        var event = new VerifiedSubscriptionEvent("evt-repeat", "subscription.charge.succeeded", "sub-1", "chr-1",
                LocalDateTime.now(), "{}");
        when(gateway.verifyEvent("evt-repeat")).thenReturn(event);
        when(eventRepository.existsByProviderAndProviderEventId("CULQI", "evt-repeat")).thenReturn(true);

        service.processCulqiEvent("evt-repeat");

        verify(subscriptionRepository, never()).findByProviderSubscriptionId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void failedChargeDoesNotGrantPaidPlan() {
        var user = player(Plan.JUGADOR_BASICO, false);
        var subscription = subscription("sub-failed", SubscriptionStatus.PENDING);
        var event = new VerifiedSubscriptionEvent("evt-failed", "subscription.charge.failed", "sub-failed", "chr-failed",
                LocalDateTime.now(), "{}");
        when(gateway.verifyEvent("evt-failed")).thenReturn(event);
        when(eventRepository.existsByProviderAndProviderEventId("CULQI", "evt-failed")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub-failed")).thenReturn(Optional.of(subscription));
        when(planRepository.findById("JUGADOR_PLUS")).thenReturn(Optional.of(plan("JUGADOR_PLUS", "JUGADOR", 1490)));
        when(paymentRepository.existsByProviderEventId("evt-failed")).thenReturn(false);
        when(userRepository.findById(new UserId(7L))).thenReturn(Optional.of(user));

        service.processCulqiEvent("evt-failed");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_BASICO);
    }

    @Test
    void legacyAccessIsNotRevokedBySubscriptionFailure() {
        var user = player(Plan.JUGADOR_PLUS, true);
        var subscription = subscription("sub-legacy", SubscriptionStatus.ACTIVE);
        var event = new VerifiedSubscriptionEvent("evt-legacy", "subscription.charge.failed", "sub-legacy", "chr-legacy",
                LocalDateTime.now(), "{}");
        when(gateway.verifyEvent("evt-legacy")).thenReturn(event);
        when(eventRepository.existsByProviderAndProviderEventId("CULQI", "evt-legacy")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub-legacy")).thenReturn(Optional.of(subscription));
        when(planRepository.findById("JUGADOR_PLUS")).thenReturn(Optional.of(plan("JUGADOR_PLUS", "JUGADOR", 1490)));
        when(paymentRepository.existsByProviderEventId("evt-legacy")).thenReturn(false);
        when(userRepository.findById(new UserId(7L))).thenReturn(Optional.of(user));

        service.processCulqiEvent("evt-legacy");

        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_PLUS);
        assertThat(user.isLegacyPlanAccess()).isTrue();
    }

    private SubscriptionPlanEntity plan(String code, String role, int amount) {
        var plan = new SubscriptionPlanEntity();
        plan.setCode(code);
        plan.setDisplayName(code);
        plan.setDescription(code);
        plan.setRole(role);
        plan.setAmountCents(amount);
        plan.setCurrency("PEN");
        plan.setIntervalUnit("MONTH");
        plan.setActive(true);
        return plan;
    }

    private SubscriptionEntity subscription(String providerId, SubscriptionStatus status) {
        var subscription = new SubscriptionEntity();
        subscription.setId(50L);
        subscription.setUserId(7L);
        subscription.setPlanCode("JUGADOR_PLUS");
        subscription.setStatus(status);
        subscription.setProvider("CULQI");
        subscription.setProviderSubscriptionId(providerId);
        subscription.setIdempotencyKey("key-" + providerId);
        return subscription;
    }

    private User player(Plan plan, boolean legacy) {
        return new User(
                new UserId(7L),
                new EmailAddress("player@test.com"),
                new FullName("Player Test"),
                new HashedPassword("hashed"),
                false,
                Role.JUGADOR,
                plan,
                com.partidoya.platform.users.models.UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                "Player",
                "Test",
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                legacy);
    }
}
