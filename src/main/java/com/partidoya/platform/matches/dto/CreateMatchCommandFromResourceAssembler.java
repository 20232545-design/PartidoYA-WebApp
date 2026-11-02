package com.partidoya.platform.matches.dto;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.CreateMatchCommand;
import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import com.partidoya.platform.matches.dto.CreateMatchResource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CreateMatchCommandFromResourceAssembler {

    private CreateMatchCommandFromResourceAssembler() {
    }

    public static CreateMatchCommand toCommandFromResource(CreateMatchResource resource) {
        return new CreateMatchCommand(
                new UserId(resource.organizerId()),
                resource.courtReservationId(),
                resource.sport(),
                new MatchTitle(resource.title()),
                new MatchDescription(resource.description()),
                new Location(resource.address()),
                new MatchDate(LocalDateTime.parse(resource.matchDate())),
                new TotalSlots(resource.totalSlots()),
                new MatchPrice(resource.price() != null ? BigDecimal.valueOf(resource.price()) : null),
                resource.latitude(),
                resource.longitude(),
                Boolean.TRUE.equals(resource.requiresPlayerPayment()),
                resource.yapePhone());
    }
}
