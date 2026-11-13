package com.partidoya.platform.realtime;

import com.partidoya.platform.matches.models.MatchId;

/** Contrato de destinos STOMP compartido entre los módulos que publican y el frontend. */
public final class RealtimeDestinations {
    public static final String ENDPOINT = "/ws";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String USER_PREFIX = "/user";

    /** Cola personal; el cliente se suscribe a {@code /user/queue/notifications}. */
    public static final String NOTIFICATIONS_QUEUE = QUEUE_PREFIX + "/notifications";

    private RealtimeDestinations() {
    }

    public static String matchChatTopic(MatchId matchId) {
        return TOPIC_PREFIX + "/matches/" + matchId.value() + "/chat";
    }
}
