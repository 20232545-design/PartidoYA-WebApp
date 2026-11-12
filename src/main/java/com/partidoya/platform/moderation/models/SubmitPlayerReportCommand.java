package com.partidoya.platform.moderation.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;

public record SubmitPlayerReportCommand(
        MatchId matchId,
        UserId reporterId,
        UserId reportedPlayerId,
        ReportReason reason,
        String details) {
}
