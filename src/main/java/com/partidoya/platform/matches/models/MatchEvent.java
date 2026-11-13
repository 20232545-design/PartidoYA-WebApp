package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;

import java.util.Objects;

/**
 * Hechos relevantes del ciclo de vida de un partido. Se publican después de aplicar el cambio
 * para que otros módulos (por ejemplo, notificaciones) reaccionen sin que {@code matches} dependa de ellos.
 */
public sealed interface MatchEvent {
    MatchId matchId();

    /** Usuario que provocó el hecho. Nunca se le notifica su propia acción. */
    UserId actorId();

    record PlayerJoined(MatchId matchId, UserId actorId) implements MatchEvent {
        public PlayerJoined {
            requireIds(matchId, actorId);
        }
    }

    record PlayerLeft(MatchId matchId, UserId actorId) implements MatchEvent {
        public PlayerLeft {
            requireIds(matchId, actorId);
        }
    }

    /** El partido pasó a tener todos sus cupos ocupados. */
    record MatchFilled(MatchId matchId, UserId actorId) implements MatchEvent {
        public MatchFilled {
            requireIds(matchId, actorId);
        }
    }

    record MatchCancelled(MatchId matchId, UserId actorId) implements MatchEvent {
        public MatchCancelled {
            requireIds(matchId, actorId);
        }
    }

    /** El organizador ({@code actorId}) aprobó el comprobante de pago de {@code playerId}. */
    record PaymentApproved(MatchId matchId, UserId actorId, UserId playerId) implements MatchEvent {
        public PaymentApproved {
            requireIds(matchId, actorId);
            Objects.requireNonNull(playerId, "playerId must not be null");
        }
    }

    /** El organizador ({@code actorId}) rechazó el comprobante de pago de {@code playerId}. */
    record PaymentRejected(MatchId matchId, UserId actorId, UserId playerId, String reason) implements MatchEvent {
        public PaymentRejected {
            requireIds(matchId, actorId);
            Objects.requireNonNull(playerId, "playerId must not be null");
        }
    }

    private static void requireIds(MatchId matchId, UserId actorId) {
        Objects.requireNonNull(matchId, "matchId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
