package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    Optional<Match> findById(MatchId id);
    Match save(Match match);
    List<Match> findAllByStatus(MatchStatus status);
    List<Match> findAllByStatusAndSport(MatchStatus status, String sport);
    List<Match> findAllByOrganizerId(UserId organizerId);
    List<Match> findAllByParticipantId(UserId participantId);
    long countRelatedToUser(UserId userId);
}
