package com.partidoya.platform.support;

import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.users.models.UserId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake en memoria que respeta el contrato de {@link MatchRepository}. */
public final class InMemoryMatchRepository implements MatchRepository {
    private final Map<MatchId, Match> matches = new HashMap<>();

    public InMemoryMatchRepository(Match... initial) {
        for (var match : initial) matches.put(match.getId(), match);
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return Optional.ofNullable(matches.get(id));
    }

    @Override
    public Match save(Match match) {
        matches.put(match.getId(), match);
        return match;
    }

    @Override
    public List<Match> findAllByStatus(MatchStatus status) {
        return matches.values().stream().filter(match -> match.getStatus() == status).toList();
    }

    @Override
    public List<Match> findAllByStatusAndSport(MatchStatus status, String sport) {
        return findAllByStatus(status).stream().filter(match -> match.getSport().equals(sport)).toList();
    }

    @Override
    public List<Match> findAllByOrganizerId(UserId organizerId) {
        return matches.values().stream().filter(match -> match.getOrganizerId().equals(organizerId)).toList();
    }

    @Override
    public List<Match> findAllByParticipantId(UserId participantId) {
        return matches.values().stream().filter(match -> match.getParticipants().contains(participantId)).toList();
    }

    @Override
    public long countRelatedToUser(UserId userId) {
        return matches.values().stream().filter(match -> match.involves(userId)).count();
    }
}
