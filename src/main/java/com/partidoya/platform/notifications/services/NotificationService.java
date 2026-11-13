package com.partidoya.platform.notifications.services;

import com.partidoya.platform.chat.models.ChatMessagePosted;
import com.partidoya.platform.chat.services.ChatPresence;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.notifications.models.MatchNotificationComposer;
import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationChanged;
import com.partidoya.platform.notifications.models.NotificationPage;
import com.partidoya.platform.notifications.models.NotificationType;
import com.partidoya.platform.notifications.repositories.NotificationRepository;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    static final int MAX_PAGE_SIZE = 50;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ChatPresence chatPresence;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, MatchRepository matchRepository,
                               UserRepository userRepository, ChatPresence chatPresence,
                               ApplicationEventPublisher eventPublisher, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.chatPresence = chatPresence;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Se ejecuta dentro de la transacción que modificó el partido: si el cambio se revierte,
     * las notificaciones también, y nunca se avisa de algo que no ocurrió.
     */
    @EventListener
    public void onMatchEvent(MatchEvent event) {
        var match = findMatch(event.matchId());
        if (match.isEmpty()) return;
        var notifications = MatchNotificationComposer.compose(event, match.get(), displayName(event.actorId()), now());
        notifications.forEach(this::saveAndAnnounce);
    }

    /**
     * Agrupa los mensajes del chat en una sola notificación por partido y destinatario.
     * No avisa al autor ni a quien ya tiene el chat abierto.
     */
    @EventListener
    public void onChatMessagePosted(ChatMessagePosted event) {
        var message = event.view().message();
        var match = findMatch(message.getMatchId());
        if (match.isEmpty()) return;
        var matchTitle = match.get().getTitle().value();
        var authorName = event.view().authorName();
        for (var recipient : membersOf(match.get())) {
            if (recipient.equals(message.getAuthorId()) || chatPresence.isViewing(recipient, message.getMatchId())) {
                continue;
            }
            // Dos mensajes simultáneos podrían crear dos avisos en vez de uno; es inocuo y el siguiente
            // mensaje vuelve a agrupar sobre el más reciente.
            var notification = notificationRepository
                    .findLatest(recipient, message.getMatchId(), NotificationType.CHAT_MESSAGES)
                    .map(existing -> {
                        existing.registerChatMessage(matchTitle, authorName, now());
                        return existing;
                    })
                    .orElseGet(() -> Notification.chatActivity(
                            recipient, message.getMatchId(), matchTitle, authorName, now()));
            saveAndAnnounce(notification);
        }
    }

    @Transactional(readOnly = true)
    public NotificationPage findForRecipient(UserId recipientId, int page, int size) {
        var safePage = Math.max(page, 0);
        var safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return new NotificationPage(
                notificationRepository.findByRecipientId(recipientId, safePage, safeSize),
                notificationRepository.countUnreadByRecipientId(recipientId),
                safePage,
                safeSize);
    }

    public Notification markAsRead(Long notificationId, UserId recipientId) {
        // Si la notificación es de otro usuario respondemos 404: no revelamos que existe.
        var notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId.toString()));
        if (notification.isRead()) return notification;
        notification.markAsRead(now());
        return notificationRepository.save(notification);
    }

    public int markAllAsRead(UserId recipientId) {
        return notificationRepository.markAllAsRead(recipientId, now());
    }

    /** Al abrir el chat de un partido, su aviso de mensajes deja de estar pendiente. */
    public int markChatAsRead(UserId recipientId, MatchId matchId) {
        return notificationRepository.markAsReadByMatchAndType(
                recipientId, matchId, NotificationType.CHAT_MESSAGES, now());
    }

    private void saveAndAnnounce(Notification notification) {
        eventPublisher.publishEvent(new NotificationChanged(notificationRepository.save(notification)));
    }

    private Optional<Match> findMatch(MatchId matchId) {
        var match = matchRepository.findById(matchId);
        if (match.isEmpty()) {
            log.warn("Ignoring notification event for missing match {}", matchId.value());
        }
        return match;
    }

    private static LinkedHashSet<UserId> membersOf(Match match) {
        var members = new LinkedHashSet<UserId>();
        members.add(match.getOrganizerId());
        members.addAll(match.getParticipants());
        return members;
    }

    private String displayName(UserId userId) {
        return userRepository.findById(userId)
                .map(user -> user.getFullName().value())
                .orElse("Un jugador");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
