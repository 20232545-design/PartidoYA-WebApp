package com.partidoya.platform.matches.dto;

import com.partidoya.platform.matches.models.PlayerJoinRequest;
import com.partidoya.platform.matches.dto.PlayerJoinRequestResource;

public final class PlayerJoinRequestResourceFromEntityAssembler {
    private PlayerJoinRequestResourceFromEntityAssembler() {
    }

    public static PlayerJoinRequestResource toResourceFromEntity(PlayerJoinRequest request, Long requesterId) {
        return new PlayerJoinRequestResource(
                request.getId(),
                request.getMatchId().value(),
                request.getPlayerId().value(),
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
