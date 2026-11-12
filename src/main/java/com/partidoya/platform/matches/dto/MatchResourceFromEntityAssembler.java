package com.partidoya.platform.matches.dto;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.dto.MatchResource;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class MatchResourceFromEntityAssembler {

    private MatchResourceFromEntityAssembler() {
    }

    /** Ids de usuario cuyos nombres necesita el recurso (organizador y participantes). */
    public static Set<UserId> playerIds(Match match) {
        var ids = new LinkedHashSet<UserId>();
        ids.add(match.getOrganizerId());
        ids.addAll(match.getParticipants());
        return ids;
    }

    public static MatchResource toResourceFromEntity(Match match, Map<Long, String> allNames) {
        var playerNames = new HashMap<Long, String>();
        for (var id : playerIds(match)) {
            var name = allNames.get(id.value());
            if (name != null) {
                playerNames.put(id.value(), name);
            }
        }
        return new MatchResource(
                match.getId().value(),
                match.getOrganizerId().value(),
                match.getSport(),
                match.getTitle().value(),
                match.getDescription() != null ? match.getDescription().value() : null,
                match.getAddress().value(),
                match.getMatchDate().value().toString(),
                match.getTotalSlots().value(),
                match.getAvailableSlots(),
                match.getPrice() != null && match.getPrice().value() != null
                        ? match.getPrice().value().doubleValue()
                        : null,
                match.getLatitude(),
                match.getLongitude(),
                match.getStatus().name(),
                match.getParticipants().stream().map(UserId::value).toList(),
                match.getCourtReservationId(),
                match.isRequiresPlayerPayment(),
                match.getYapePhone(),
                match.getPlayerPaymentAmount() != null && match.getPlayerPaymentAmount().value() != null
                        ? match.getPlayerPaymentAmount().value().doubleValue()
                        : null,
                playerNames);
    }
}
