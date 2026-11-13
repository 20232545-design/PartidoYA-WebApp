package com.partidoya.platform.chat.services;

import com.partidoya.platform.chat.models.ChatMessage;
import com.partidoya.platform.chat.models.ChatMessagePosted;
import com.partidoya.platform.chat.models.ChatMessageView;
import com.partidoya.platform.chat.repositories.ChatMessageRepository;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.partidoya.platform.support.MatchFixtures.match;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchChatServiceTest {
    private static final MatchId OPEN_MATCH = new MatchId(1L);
    private static final MatchId CANCELLED_MATCH = new MatchId(2L);
    private static final UserId ORGANIZER = new UserId(1L);
    private static final UserId PLAYER = new UserId(7L);
    private static final UserId OUTSIDER = new UserId(99L);

    private InMemoryChatMessageRepository messageRepository;
    private RecordingEventPublisher eventPublisher;
    private MatchChatService service;

    @BeforeEach
    void setUp() {
        messageRepository = new InMemoryChatMessageRepository();
        eventPublisher = new RecordingEventPublisher();
        var matchRepository = new InMemoryMatchRepository(
                match(OPEN_MATCH, ORGANIZER, List.of(PLAYER), MatchStatus.OPEN),
                match(CANCELLED_MATCH, ORGANIZER, List.of(PLAYER), MatchStatus.CANCELLED));
        var userRepository = new InMemoryUserRepository()
                .withPlayer(ORGANIZER, "Carla Organizadora")
                .withPlayer(PLAYER, "Ana Torres");
        service = new MatchChatService(messageRepository, matchRepository, userRepository, eventPublisher,
                Clock.fixed(Instant.parse("2026-11-12T18:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void participantPostsAMessageThatIsSavedAndAnnounced() {
        var view = service.post(OPEN_MATCH, PLAYER, "  ¿Quién lleva pelota?  ");

        assertThat(view.message().getId()).isNotNull();
        assertThat(view.message().getContent()).isEqualTo("¿Quién lleva pelota?");
        assertThat(view.message().getSentAt()).isEqualTo(LocalDateTime.of(2026, 11, 12, 18, 0));
        assertThat(view.authorName()).isEqualTo("Ana Torres");
        assertThat(eventPublisher.eventsOfType(ChatMessagePosted.class))
                .singleElement()
                .extracting(ChatMessagePosted::view)
                .isEqualTo(view);
    }

    @Test
    void organizerCanAlsoPost() {
        var view = service.post(OPEN_MATCH, ORGANIZER, "Confirmado, nos vemos");

        assertThat(view.authorName()).isEqualTo("Carla Organizadora");
    }

    @Test
    void outsiderCannotPostOrReadOrSubscribe() {
        assertThatThrownBy(() -> service.post(OPEN_MATCH, OUTSIDER, "Hola"))
                .isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> service.history(OPEN_MATCH, OUTSIDER, null, null))
                .isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> service.requireReadAccess(OPEN_MATCH, OUTSIDER))
                .isInstanceOf(ForbiddenActionException.class);
        assertThat(messageRepository.count()).isZero();
        assertThat(eventPublisher.eventsOfType(ChatMessagePosted.class)).isEmpty();
    }

    @Test
    void cancelledMatchKeepsItsHistoryReadableButRejectsNewMessages() {
        assertThatThrownBy(() -> service.post(CANCELLED_MATCH, PLAYER, "¿Se juega?"))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> service.history(CANCELLED_MATCH, PLAYER, null, null)).doesNotThrowAnyException();
    }

    @Test
    void unknownMatchIsReportedAsNotFound() {
        assertThatThrownBy(() -> service.post(new MatchId(404L), PLAYER, "Hola"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyIsChronologicalAndPagesBackwardsFromACursor() {
        for (var index = 1; index <= 5; index++) {
            service.post(OPEN_MATCH, index % 2 == 0 ? ORGANIZER : PLAYER, "Mensaje " + index);
        }

        var latest = service.history(OPEN_MATCH, PLAYER, null, 3);
        var older = service.history(OPEN_MATCH, PLAYER, latest.getFirst().message().getId(), 3);

        assertThat(latest).extracting(view -> view.message().getContent())
                .containsExactly("Mensaje 3", "Mensaje 4", "Mensaje 5");
        assertThat(older).extracting(view -> view.message().getContent())
                .containsExactly("Mensaje 1", "Mensaje 2");
        assertThat(latest).extracting(ChatMessageView::authorName)
                .containsExactly("Ana Torres", "Carla Organizadora", "Ana Torres");
    }

    @Test
    void historyLimitIsClampedToTheMaximum() {
        for (var index = 0; index < MatchChatService.MAX_HISTORY_LIMIT + 5; index++) {
            service.post(OPEN_MATCH, PLAYER, "m" + index);
        }

        assertThat(service.history(OPEN_MATCH, PLAYER, null, 5_000)).hasSize(MatchChatService.MAX_HISTORY_LIMIT);
        assertThat(service.history(OPEN_MATCH, PLAYER, null, 0)).hasSize(1);
    }

    /** Fake en memoria que respeta el contrato de {@link ChatMessageRepository}. */
    private static final class InMemoryChatMessageRepository implements ChatMessageRepository {
        private final List<ChatMessage> messages = new ArrayList<>();
        private long nextId = 1;

        long count() {
            return messages.size();
        }

        @Override
        public ChatMessage save(ChatMessage message) {
            var stored = ChatMessage.restore(nextId++, message.getMatchId(), message.getAuthorId(),
                    message.getContent(), message.getSentAt());
            messages.add(stored);
            return stored;
        }

        @Override
        public List<ChatMessage> findLatestByMatchId(MatchId matchId, Long beforeId, int limit) {
            return messages.stream()
                    .filter(message -> message.getMatchId().equals(matchId))
                    .filter(message -> beforeId == null || message.getId() < beforeId)
                    .sorted(Comparator.comparing(ChatMessage::getId).reversed())
                    .limit(limit)
                    .toList();
        }
    }
}
