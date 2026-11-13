package com.partidoya.platform.matches.dto;

import com.partidoya.platform.matches.models.PlayerJoinRequest;

import java.util.Map;

public final class PlayerJoinRequestResourceFromEntityAssembler {
    private PlayerJoinRequestResourceFromEntityAssembler() {
    }

    /**
     * @param playerNames nombres visibles por id de usuario; si falta el del jugador se usa "Jugador #id"
     */
    public static PlayerJoinRequestResource toResourceFromEntity(PlayerJoinRequest request, Long requesterId,
                                                                 Map<Long, String> playerNames) {
        var playerId = request.getPlayerId().value();
        return new PlayerJoinRequestResource(
                request.getId(),
                request.getMatchId().value(),
                playerId,
                playerNames.getOrDefault(playerId, "Jugador #" + playerId),
                "/api/v1/matches/%d/join-requests/%d/proof?requesterId=%d"
                        .formatted(request.getMatchId().value(), request.getId(), requesterId),
                request.getOriginalFileName(),
                request.getAmount(),
                request.getStatus().name(),
                request.getSubmittedAt(),
                request.getReviewedAt(),
                request.getReviewedBy() != null ? request.getReviewedBy().value() : null,
                request.getRejectionReason());
    }
}
