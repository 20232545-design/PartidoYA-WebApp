package com.partidoya.platform.chat.services;

import com.partidoya.platform.chat.models.ChatMessage;
import com.partidoya.platform.chat.models.ChatMessagePosted;
import com.partidoya.platform.chat.models.ChatMessageView;
import com.partidoya.platform.chat.repositories.ChatMessageRepository;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Chat grupal de un partido: solo el organizador y los jugadores inscritos pueden leer y escribir. */
@Service
@Transactional
public class MatchChatService {
    static final int DEFAULT_HISTORY_LIMIT = 50;
    static final int MAX_HISTORY_LIMIT = 100;

    private final ChatMessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MatchChatService(ChatMessageRepository messageRepository, MatchRepository matchRepository,
                            UserRepository userRepository, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public ChatMessageView post(MatchId matchId, UserId authorId, String content) {
        var match = requireMember(matchId, authorId);
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot send messages to a cancelled match");
        }
        var message = ChatMessage.post(matchId, authorId, content, LocalDateTime.now(clock));
        var view = new ChatMessageView(messageRepository.save(message), displayNames(List.of(message)).get(authorId));
        eventPublisher.publishEvent(new ChatMessagePosted(view));
        return view;
    }

    /**
     * Historial en orden cronológico (el más antiguo primero), listo para pintarse.
     *
     * @param beforeId si no es nulo, devuelve los mensajes anteriores a ese id
     */
    @Transactional(readOnly = true)
    public List<ChatMessageView> history(MatchId matchId, UserId requesterId, Long beforeId, Integer limit) {
        requireMember(matchId, requesterId);
        var safeLimit = limit == null ? DEFAULT_HISTORY_LIMIT : Math.clamp(limit, 1, MAX_HISTORY_LIMIT);
        var newestFirst = messageRepository.findLatestByMatchId(matchId, beforeId, safeLimit);
        var names = displayNames(newestFirst);
        return newestFirst.reversed().stream()
                .map(message -> new ChatMessageView(message, names.get(message.getAuthorId())))
                .toList();
    }

    /** Usado al autorizar la suscripción WebSocket al chat del partido. */
    @Transactional(readOnly = true)
    public void requireReadAccess(MatchId matchId, UserId userId) {
        requireMember(matchId, userId);
    }

    private Match requireMember(MatchId matchId, UserId userId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.value().toString()));
        if (!match.involves(userId)) {
            throw new ForbiddenActionException("Only the organizer and joined players can use this chat");
        }
        return match;
    }

    /** Siempre devuelve un nombre por autor, aunque el usuario ya no exista. */
    private Map<UserId, String> displayNames(List<ChatMessage> messages) {
        var authorIds = messages.stream().map(ChatMessage::getAuthorId).distinct().toList();
        var found = userRepository.findAllByIds(authorIds).stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user.getFullName().value(),
                        (first, second) -> first));
        return authorIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> found.getOrDefault(id, "Jugador #" + id.value())));
    }
}
