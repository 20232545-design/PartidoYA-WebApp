package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;

import java.time.LocalDateTime;
import java.util.Objects;

public record CreateMatchCommand(
        UserId organizerId,
        Long courtReservationId,
        String sport,
        MatchTitle title,
        MatchDescription description,
        Location address,
        MatchDate matchDate,
        TotalSlots totalSlots,
        MatchPrice price,
        Double latitude,
        Double longitude,
        boolean requiresPlayerPayment,
        String yapePhone) {
    public CreateMatchCommand {
        Objects.requireNonNull(organizerId, "organizerId cannot be null");
        Objects.requireNonNull(sport, "sport cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(address, "address cannot be null");
        Objects.requireNonNull(matchDate, "matchDate cannot be null");
        Objects.requireNonNull(totalSlots, "totalSlots cannot be null");
        if (matchDate.value().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("matchDate must be in the future");
        }
    }
}
