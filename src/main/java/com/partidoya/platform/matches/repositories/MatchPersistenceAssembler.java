package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import com.partidoya.platform.matches.repositories.MatchPersistenceEntity;

import java.util.ArrayList;

public final class MatchPersistenceAssembler {

    private MatchPersistenceAssembler() {
    }

    public static Match toDomainFromPersistence(MatchPersistenceEntity entity) {
        if (entity == null) return null;
        var participants = entity.getParticipantIds().stream()
                .map(UserId::new)
                .toList();
        return new Match(
                new MatchId(entity.getId()),
                new UserId(entity.getOrganizerId()),
                entity.getCourtReservationId(),
                entity.getSport(),
                new MatchTitle(entity.getTitle()),
                new MatchDescription(entity.getDescription()),
                new Location(entity.getAddress()),
                new MatchDate(entity.getMatchDate()),
                new TotalSlots(entity.getTotalSlots()),
                new MatchPrice(entity.getPrice()),
                entity.getLatitude(),
                entity.getLongitude(),
                MatchStatus.valueOf(entity.getStatus()),
                participants,
                entity.isRequiresPlayerPayment(),
                entity.getYapePhone(),
                new MatchPrice(entity.getPlayerPaymentAmount()));
    }

    public static MatchPersistenceEntity toPersistenceFromDomain(Match match) {
        if (match == null) return null;
        var entity = new MatchPersistenceEntity();
        if (match.getId() != null) {
            entity.setId(match.getId().value());
        }
        entity.setOrganizerId(match.getOrganizerId().value());
        entity.setCourtReservationId(match.getCourtReservationId());
        entity.setSport(match.getSport());
        entity.setTitle(match.getTitle().value());
        entity.setDescription(match.getDescription() != null ? match.getDescription().value() : null);
        entity.setAddress(match.getAddress().value());
        entity.setMatchDate(match.getMatchDate().value());
        entity.setTotalSlots(match.getTotalSlots().value());
        entity.setPrice(match.getPrice() != null ? match.getPrice().value() : null);
        entity.setLatitude(match.getLatitude());
        entity.setLongitude(match.getLongitude());
        entity.setStatus(match.getStatus().name());
        entity.setRequiresPlayerPayment(match.isRequiresPlayerPayment());
        entity.setYapePhone(match.getYapePhone());
        entity.setPlayerPaymentAmount(match.getPlayerPaymentAmount() != null ? match.getPlayerPaymentAmount().value() : null);
        var participantIds = new ArrayList<>(match.getParticipants().stream()
                .map(UserId::value)
                .toList());
        entity.setParticipantIds(participantIds);
        return entity;
    }
}
