package com.partidoya.platform.notifications.models;

import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.users.models.UserId;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Decide quién recibe un aviso por cada {@link MatchEvent} y con qué texto.
 * Es lógica pura (sin I/O) para poder probar todas las reglas de destinatarios de forma aislada.
 */
public final class MatchNotificationComposer {
    static final int MAX_REASON_LENGTH = 150;

    private MatchNotificationComposer() {
    }

    /**
     * @param actorName nombre visible de quien provocó el evento
     * @return avisos para cada destinatario; quien provocó el evento nunca recibe el suyo
     */
    public static List<Notification> compose(MatchEvent event, Match match, String actorName, LocalDateTime now) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(match, "match must not be null");
        if (!event.matchId().equals(match.getId())) {
            throw new IllegalArgumentException("event and match must refer to the same match");
        }
        var title = "«" + match.getTitle().value() + "»";
        var draft = switch (event) {
            case MatchEvent.PlayerJoined ignored -> new Draft(
                    Set.of(match.getOrganizerId()),
                    NotificationType.PLAYER_JOINED,
                    "%s se unió a %s.".formatted(actorName, title));
            case MatchEvent.PlayerLeft ignored -> new Draft(
                    Set.of(match.getOrganizerId()),
                    NotificationType.PLAYER_LEFT,
                    "%s abandonó %s. Se liberó un cupo.".formatted(actorName, title));
            case MatchEvent.MatchFilled ignored -> new Draft(
                    everyoneIn(match),
                    NotificationType.MATCH_FULL,
                    "%s ya está completo. ¡Se juega!".formatted(title));
            case MatchEvent.MatchCancelled ignored -> new Draft(
                    everyoneIn(match),
                    NotificationType.MATCH_CANCELLED,
                    "El organizador canceló %s.".formatted(title));
            case MatchEvent.PaymentApproved approved -> new Draft(
                    Set.of(approved.playerId()),
                    NotificationType.PAYMENT_APPROVED,
                    "Tu pago para %s fue aprobado. Ya estás inscrito.".formatted(title));
            case MatchEvent.PaymentRejected rejected -> new Draft(
                    Set.of(rejected.playerId()),
                    NotificationType.PAYMENT_REJECTED,
                    "Tu comprobante para %s fue rechazado.%s".formatted(title, reasonSuffix(rejected.reason())));
        };
        return draft.recipients().stream()
                .filter(recipient -> !recipient.equals(event.actorId()))
                .map(recipient -> Notification.create(recipient, match.getId(), draft.type(), draft.message(), now))
                .toList();
    }

    private static Set<UserId> everyoneIn(Match match) {
        var recipients = new LinkedHashSet<UserId>();
        recipients.add(match.getOrganizerId());
        recipients.addAll(match.getParticipants());
        return recipients;
    }

    private static String reasonSuffix(String reason) {
        if (reason == null || reason.isBlank()) return "";
        var trimmed = reason.trim();
        var shortened = trimmed.length() > MAX_REASON_LENGTH
                ? trimmed.substring(0, MAX_REASON_LENGTH - 1) + "…"
                : trimmed;
        return " Motivo: " + shortened;
    }

    private record Draft(Set<UserId> recipients, NotificationType type, String message) {
    }
}
