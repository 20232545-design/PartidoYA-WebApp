package com.partidoya.platform.chat.services;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;

/** Indica si un usuario está viendo ahora mismo el chat de un partido (y por lo tanto no necesita aviso). */
public interface ChatPresence {
    boolean isViewing(UserId userId, MatchId matchId);
}
