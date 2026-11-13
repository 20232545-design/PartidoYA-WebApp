package com.partidoya.platform.support;

import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import com.partidoya.platform.users.models.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class MatchFixtures {
    private MatchFixtures() {
    }

    public static Match match(MatchId id, UserId organizer, List<UserId> participants, MatchStatus status) {
        return new Match(id, organizer, "Futbol", new MatchTitle("Pichanga"), new MatchDescription("Test"),
                new Location("Lima"), new MatchDate(LocalDateTime.of(2026, 11, 20, 18, 0)), new TotalSlots(10),
                new MatchPrice(BigDecimal.ZERO), null, null, status, participants);
    }
}
