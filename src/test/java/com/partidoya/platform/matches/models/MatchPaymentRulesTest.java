package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.CreateMatchCommand;
import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchPaymentRulesTest {
    @Test
    void calculatesPlayerPaymentIncludingOrganizer() {
        var match = new Match(command(true, "999999999", 10, "200.00"));

        assertThat(match.isRequiresPlayerPayment()).isTrue();
        assertThat(match.getPlayerPaymentAmount().value()).isEqualByComparingTo("20.00");
    }

    @Test
    void rejectsInvalidYapePhoneWhenPaymentIsRequired() {
        assertThatThrownBy(() -> new Match(command(true, "12345", 10, "200.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blocksDirectJoinWhenPaymentProofIsRequired() {
        var match = new Match(command(true, "999999999", 10, "200.00"));

        assertThatThrownBy(() -> match.join(new UserId(22L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blocksCapacityOverflowAndDuplicatedParticipant() {
        var match = new Match(command(false, null, 2, "0"));
        match.join(new UserId(2L));
        match.join(new UserId(3L));

        assertThatThrownBy(() -> match.join(new UserId(4L))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> match.join(new UserId(2L))).isInstanceOf(IllegalStateException.class);
    }

    private static CreateMatchCommand command(boolean requiresPayment, String yapePhone, int slots, String price) {
        return new CreateMatchCommand(
                new UserId(1L),
                10L,
                "Futbol",
                new MatchTitle("Pichanga"),
                new MatchDescription("Test"),
                new Location("Lima"),
                new MatchDate(LocalDateTime.now().plusDays(1)),
                new TotalSlots(slots),
                new MatchPrice(new BigDecimal(price)),
                null,
                null,
                requiresPayment,
                yapePhone);
    }
}
