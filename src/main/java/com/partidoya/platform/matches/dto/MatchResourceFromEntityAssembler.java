package com.partidoya.platform.matches.dto;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.dto.MatchResource;

public final class MatchResourceFromEntityAssembler {

    private MatchResourceFromEntityAssembler() {
    }

    public static MatchResource toResourceFromEntity(Match match) {
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
                        : null);
    }
}
