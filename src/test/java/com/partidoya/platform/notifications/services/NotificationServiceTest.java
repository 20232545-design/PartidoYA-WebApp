package com.partidoya.platform.notifications.services;

import com.partidoya.platform.chat.models.ChatMessage;
import com.partidoya.platform.chat.models.ChatMessagePosted;
import com.partidoya.platform.chat.models.ChatMessageView;
import com.partidoya.platform.chat.services.ChatPresence;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationChanged;
import com.partidoya.platform.notifications.models.NotificationType;
import com.partidoya.platform.notifications.repositories.NotificationRepository;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.support.InMemoryMatchRepository;
import com.partidoya.platform.support.InMemoryUserRepository;
import com.partidoya.platform.support.RecordingEventPublisher;
import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.partidoya.platform.support.MatchFixtures.match;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 18, 0);
    private static final MatchId MATCH_ID = new MatchId(1L);
    private static final UserId ORGANIZER = new UserId(1L);
    private static final UserId PLAYER_A = new UserId(7L);
    private static final UserId PLAYER_B = new UserId(18L);

    private InMemoryNotificationRepository notificationRepository;
    private RecordingEventPublisher eventPublisher;
    private FakeChatPresence chatPresence;
    private NotificationService service;
    private long nextMessageId = 1;

    @BeforeEach
    void setUp() {
        notificationRepository = new InMemoryNotificationRepository();
        eventPublisher = new RecordingEventPublisher();
        chatPresence = new FakeChatPresence();
        var matchRepository = new InMemoryMatchRepository(
                match(MATCH_ID, ORGANIZER, List.of(PLAYER_A, PLAYER_B), MatchStatus.OPEN));
        var userRepository = new InMemoryUserRepository().withPlayer(PLAYER_A, "Ana Torres");
        service = new NotificationService(notificationRepository, matchRepository, userRepository, chatPresence,
                eventPublisher, Clock.fixed(Instant.parse("2026-11-12T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void savesAndAnnouncesOneNotificationPerRecipient() {
        service.onMatchEvent(new MatchEvent.MatchCancelled(MATCH_ID, ORGANIZER));

        assertThat(notificationRepository.all()).extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(PLAYER_A, PLAYER_B);
        assertThat(eventPublisher.eventsOfType(NotificationChanged.class))
                .extracting(event -> event.notification().getId())
                .containsExactlyInAnyOrderElementsOf(notificationRepository.all().stream().map(Notification::getId).toList());
    }

    @Test
    void usesTheActorNameInTheMessage() {
        service.onMatchEvent(new MatchEvent.PlayerJoined(MATCH_ID, PLAYER_A));

        assertThat(notificationRepository.all()).singleElement()
                .extracting(Notification::getMessage)
                .isEqualTo("Ana Torres se unió a «Pichanga».");
    }

    @Test
    void fallsBackToAGenericNameWhenTheActorNoLongerExists() {
        service.onMatchEvent(new MatchEvent.PlayerLeft(MATCH_ID, PLAYER_B));

        assertThat(notificationRepository.all()).singleElement()
                .extracting(Notification::getMessage)
                .asString()
                .startsWith("Un jugador abandonó");
    }

    @Test
    void ignoresEventsForMatchesThatDoNotExist() {
        service.onMatchEvent(new MatchEvent.MatchCancelled(new MatchId(404L), ORGANIZER));

        assertThat(notificationRepository.all()).isEmpty();
        assertThat(eventPublisher.eventsOfType(NotificationChanged.class)).isEmpty();
    }

    @Test
    void chatMessageNotifiesEveryMemberExceptTheAuthor() {
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));

        assertThat(notificationRepository.all()).extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(ORGANIZER, PLAYER_B);
        assertThat(notificationRepository.all()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.CHAT_MESSAGES);
            assertThat(notification.getMessage()).isEqualTo("Ana Torres escribió en el chat de «Pichanga».");
        });
        assertThat(eventPublisher.eventsOfType(NotificationChanged.class)).hasSize(2);
    }

    @Test
    void consecutiveChatMessagesAreGroupedIntoOneNotificationPerRecipient() {
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));
        service.onChatMessagePosted(chatMessageFrom(PLAYER_B, "Luis"));
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));

        var organizerNotifications = notificationsOf(ORGANIZER);
        assertThat(organizerNotifications).singleElement().satisfies(notification -> {
            assertThat(notification.getGroupedCount()).isEqualTo(3);
            assertThat(notification.getMessage()).isEqualTo("3 mensajes nuevos en el chat de «Pichanga».");
        });
        // Cada jugador solo cuenta los mensajes de los demás.
        assertThat(notificationsOf(PLAYER_A)).singleElement()
                .extracting(Notification::getGroupedCount).isEqualTo(1);
        assertThat(notificationsOf(PLAYER_B)).singleElement()
                .extracting(Notification::getGroupedCount).isEqualTo(2);
    }

    @Test
    void membersWhoHaveTheChatOpenAreNotNotified() {
        chatPresence.viewing.add(ORGANIZER);

        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));

        assertThat(notificationRepository.all()).extracting(Notification::getRecipientId).containsExactly(PLAYER_B);
    }

    @Test
    void openingTheChatMarksItsNotificationAsReadAndTheNextMessageStartsANewCount() {
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));

        var updated = service.markChatAsRead(ORGANIZER, MATCH_ID);
        service.onChatMessagePosted(chatMessageFrom(PLAYER_B, "Luis"));

        assertThat(updated).isEqualTo(1);
        assertThat(notificationsOf(ORGANIZER)).singleElement().satisfies(notification -> {
            assertThat(notification.isRead()).isFalse();
            assertThat(notification.getGroupedCount()).isEqualTo(1);
            assertThat(notification.getMessage()).isEqualTo("Luis escribió en el chat de «Pichanga».");
        });
    }

    @Test
    void markingTheChatAsReadDoesNotTouchOtherNotifications() {
        service.onMatchEvent(new MatchEvent.PlayerJoined(MATCH_ID, PLAYER_A));
        service.onChatMessagePosted(chatMessageFrom(PLAYER_A, "Ana Torres"));

        service.markChatAsRead(ORGANIZER, MATCH_ID);

        assertThat(service.findForRecipient(ORGANIZER, 0, 20).unreadCount()).isEqualTo(1);
    }

    @Test
    void listsOnlyTheRecipientsNotificationsNewestFirstWithUnreadCount() {
        service.onMatchEvent(new MatchEvent.PaymentApproved(MATCH_ID, ORGANIZER, PLAYER_A));
        service.onMatchEvent(new MatchEvent.MatchCancelled(MATCH_ID, ORGANIZER));

        var page = service.findForRecipient(PLAYER_A, 0, 20);

        assertThat(page.content()).extracting(Notification::getType)
                .containsExactly(NotificationType.MATCH_CANCELLED, NotificationType.PAYMENT_APPROVED);
        assertThat(page.unreadCount()).isEqualTo(2);
    }

    @Test
    void clampsPageSizeToTheMaximum() {
        var page = service.findForRecipient(PLAYER_A, -3, 10_000);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(NotificationService.MAX_PAGE_SIZE);
    }

    @Test
    void recipientCanMarkTheirNotificationAsRead() {
        service.onMatchEvent(new MatchEvent.PaymentApproved(MATCH_ID, ORGANIZER, PLAYER_A));
        var id = notificationRepository.all().getFirst().getId();

        var read = service.markAsRead(id, PLAYER_A);

        assertThat(read.getReadAt()).isEqualTo(NOW);
        assertThat(service.findForRecipient(PLAYER_A, 0, 20).unreadCount()).isZero();
    }

    @Test
    void cannotMarkSomeoneElsesNotificationAsRead() {
        service.onMatchEvent(new MatchEvent.PaymentApproved(MATCH_ID, ORGANIZER, PLAYER_A));
        var id = notificationRepository.all().getFirst().getId();

        assertThatThrownBy(() -> service.markAsRead(id, PLAYER_B)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(notificationRepository.all().getFirst().isRead()).isFalse();
    }

    @Test
    void markAllAsReadOnlyAffectsTheRecipient() {
        service.onMatchEvent(new MatchEvent.MatchCancelled(MATCH_ID, ORGANIZER));

        var updated = service.markAllAsRead(PLAYER_A);

        assertThat(updated).isEqualTo(1);
        assertThat(service.findForRecipient(PLAYER_A, 0, 20).unreadCount()).isZero();
        assertThat(service.findForRecipient(PLAYER_B, 0, 20).unreadCount()).isEqualTo(1);
    }

    private List<Notification> notificationsOf(UserId recipient) {
        return notificationRepository.all().stream()
                .filter(notification -> notification.getRecipientId().equals(recipient))
                .toList();
    }

    private ChatMessagePosted chatMessageFrom(UserId author, String authorName) {
        var message = ChatMessage.restore(nextMessageId++, MATCH_ID, author, "Hola", NOW);
        return new ChatMessagePosted(new ChatMessageView(message, authorName));
    }

    private static final class FakeChatPresence implements ChatPresence {
        private final Set<UserId> viewing = new HashSet<>();

        @Override
        public boolean isViewing(UserId userId, MatchId matchId) {
            return viewing.contains(userId);
        }
    }

    /** Fake en memoria que respeta el contrato de {@link NotificationRepository}. */
    private static final class InMemoryNotificationRepository implements NotificationRepository {
        private static final Comparator<Notification> NEWEST_FIRST = Comparator
                .comparing(Notification::getOccurredAt)
                .thenComparing(Notification::getId)
                .reversed();

        private final Map<Long, Notification> notifications = new LinkedHashMap<>();
        private long nextId = 1;

        List<Notification> all() {
            return List.copyOf(notifications.values());
        }

        @Override
        public Notification save(Notification notification) {
            var id = notification.getId() != null ? notification.getId() : nextId++;
            var stored = Notification.restore(id, notification.getRecipientId(), notification.getMatchId(),
                    notification.getType(), notification.getMessage(), notification.getOccurredAt(),
                    notification.getReadAt(), notification.getGroupedCount());
            notifications.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Notification> findByIdAndRecipientId(Long id, UserId recipientId) {
            return Optional.ofNullable(notifications.get(id))
                    .filter(notification -> notification.getRecipientId().equals(recipientId))
                    .map(this::copy);
        }

        @Override
        public Optional<Notification> findLatest(UserId recipientId, MatchId matchId, NotificationType type) {
            return notifications.values().stream()
                    .filter(notification -> notification.getRecipientId().equals(recipientId)
                            && notification.getMatchId().equals(matchId)
                            && notification.getType() == type)
                    .max(Comparator.comparing(Notification::getId))
                    .map(this::copy);
        }

        @Override
        public List<Notification> findByRecipientId(UserId recipientId, int page, int size) {
            return notifications.values().stream()
                    .filter(notification -> notification.getRecipientId().equals(recipientId))
                    .sorted(NEWEST_FIRST)
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
        }

        @Override
        public long countUnreadByRecipientId(UserId recipientId) {
            return notifications.values().stream()
                    .filter(notification -> notification.getRecipientId().equals(recipientId) && !notification.isRead())
                    .count();
        }

        @Override
        public int markAllAsRead(UserId recipientId, LocalDateTime readAt) {
            return markWhere(notification -> notification.getRecipientId().equals(recipientId), readAt);
        }

        @Override
        public int markAsReadByMatchAndType(UserId recipientId, MatchId matchId, NotificationType type,
                                            LocalDateTime readAt) {
            return markWhere(notification -> notification.getRecipientId().equals(recipientId)
                    && notification.getMatchId().equals(matchId)
                    && notification.getType() == type, readAt);
        }

        private int markWhere(Predicate<Notification> filter, LocalDateTime readAt) {
            var unread = notifications.values().stream().filter(filter).filter(n -> !n.isRead()).toList();
            unread.forEach(notification -> notification.markAsRead(readAt));
            return unread.size();
        }

        /** Como una base de datos real: los cambios solo persisten al llamar a save. */
        private Notification copy(Notification source) {
            return Notification.restore(source.getId(), source.getRecipientId(), source.getMatchId(), source.getType(),
                    source.getMessage(), source.getOccurredAt(), source.getReadAt(), source.getGroupedCount());
        }
    }
}
