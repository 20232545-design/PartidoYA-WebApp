package com.partidoya.platform.chat.realtime;

import com.partidoya.platform.chat.services.ChatPresence;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.realtime.RealtimeDestinations;
import com.partidoya.platform.users.models.UserId;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Un usuario "está viendo" el chat si alguna de sus sesiones WebSocket (cualquier pestaña)
 * está suscrita al tópico del chat del partido.
 */
@Component
public class StompChatPresence implements ChatPresence {
    private final SimpUserRegistry userRegistry;

    public StompChatPresence(SimpUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public boolean isViewing(UserId userId, MatchId matchId) {
        var user = userRegistry.getUser(userId.value().toString());
        if (user == null) return false;
        var topic = RealtimeDestinations.matchChatTopic(matchId);
        return user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .anyMatch(subscription -> topic.equals(subscription.getDestination()));
    }
}
