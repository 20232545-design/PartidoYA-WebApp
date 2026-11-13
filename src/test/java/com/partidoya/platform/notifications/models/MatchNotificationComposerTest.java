package com.partidoya.platform.notifications.models;

import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.partidoya.platform.support.MatchFixtures.match;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchNotificationComposerTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 18, 0);
    private static final MatchId MATCH_ID = new MatchId(1L);
    private static final UserId ORGANIZER = new UserId(1L);
    private static final UserId PLAYER_A = new UserId(7L);
    private static final UserId PLAYER_B = new UserId(18L);

    private final Match match = match(MATCH_ID, ORGANIZER, List.of(PLAYER_A, PLAYER_B), MatchStatus.OPEN);

    @Test
    void playerJoinedNotifiesOnlyTheOrganizer() {
        var notifications = compose(new MatchEvent.PlayerJoined(MATCH_ID, PLAYER_A), "Ana Torres");

        assertThat(notifications).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientId()).isEqualTo(ORGANIZER);
            assertThat(notification.getType()).isEqualTo(NotificationType.PLAYER_JOINED);
            assertThat(notification.getMessage()).isEqualTo("Ana Torres se unió a «Pichanga».");
            assertThat(notification.getOccurredAt()).isEqualTo(NOW);
            assertThat(notification.isRead()).isFalse();
        });
    }

    @Test
    void playerLeftNotifiesTheOrganizerThatASlotOpened() {
        var notifications = compose(new MatchEvent.PlayerLeft(MATCH_ID, PLAYER_B), "Luis");

        assertThat(notifications).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientId()).isEqualTo(ORGANIZER);
            assertThat(notification.getType()).isEqualTo(NotificationType.PLAYER_LEFT);
            assertThat(notification.getMessage()).contains("Luis abandonó «Pichanga»");
        });
    }

    @Test
    void cancellationNotifiesEveryParticipantButNotTheOrganizerWhoCancelled() {
        var notifications = compose(new MatchEvent.MatchCancelled(MATCH_ID, ORGANIZER), "Organizador");

        assertThat(notifications).extracting(Notification::getRecipientId).containsExactly(PLAYER_A, PLAYER_B);
        assertThat(notifications).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.MATCH_CANCELLED);
            assertThat(notification.getMessage()).isEqualTo("El organizador canceló «Pichanga».");
        });
    }

    @Test
    void matchFilledNotifiesOrganizerAndOtherPlayersButNotThePlayerWhoFilledIt() {
        var notifications = compose(new MatchEvent.MatchFilled(MATCH_ID, PLAYER_B), "Luis");

        assertThat(notifications).extracting(Notification::getRecipientId).containsExactly(ORGANIZER, PLAYER_A);
        assertThat(notifications).allSatisfy(notification ->
                assertThat(notification.getType()).isEqualTo(NotificationType.MATCH_FULL));
    }

    @Test
    void paymentApprovalConfirmsTheSpotToThePlayer() {
        var notifications = compose(new MatchEvent.PaymentApproved(MATCH_ID, ORGANIZER, PLAYER_A), "Organizador");

        assertThat(notifications).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientId()).isEqualTo(PLAYER_A);
            assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_APPROVED);
            assertThat(notification.getMessage()).contains("Ya estás inscrito");
        });
    }

    @Test
    void paymentRejectionIncludesTheReasonWhenPresent() {
        var withReason = compose(new MatchEvent.PaymentRejected(MATCH_ID, ORGANIZER, PLAYER_A, " Monto incorrecto "), "x");
        var withoutReason = compose(new MatchEvent.PaymentRejected(MATCH_ID, ORGANIZER, PLAYER_A, "  "), "x");

        assertThat(withReason.getFirst().getMessage())
                .isEqualTo("Tu comprobante para «Pichanga» fue rechazado. Motivo: Monto incorrecto");
        assertThat(withoutReason.getFirst().getMessage()).isEqualTo("Tu comprobante para «Pichanga» fue rechazado.");
    }

    @Test
    void veryLongRejectionReasonIsShortenedToKeepTheMessageValid() {
        var longReason = "a".repeat(2_000);

        var notifications = compose(new MatchEvent.PaymentRejected(MATCH_ID, ORGANIZER, PLAYER_A, longReason), "x");

        assertThat(notifications.getFirst().getMessage())
                .hasSizeLessThanOrEqualTo(Notification.MAX_MESSAGE_LENGTH)
                .endsWith("…");
    }

    @Test
    void cancellingAMatchWithoutPlayersProducesNoNotifications() {
        var emptyMatch = match(MATCH_ID, ORGANIZER, List.of(), MatchStatus.OPEN);

        var notifications = MatchNotificationComposer.compose(
                new MatchEvent.MatchCancelled(MATCH_ID, ORGANIZER), emptyMatch, "Organizador", NOW);

        assertThat(notifications).isEmpty();
    }

    @Test
    void rejectsAnEventThatBelongsToAnotherMatch() {
        var otherMatchEvent = new MatchEvent.PlayerJoined(new MatchId(2L), PLAYER_A);

        assertThatThrownBy(() -> MatchNotificationComposer.compose(otherMatchEvent, match, "Ana", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private List<Notification> compose(MatchEvent event, String actorName) {
        return MatchNotificationComposer.compose(event, match, actorName, NOW);
    }
}
