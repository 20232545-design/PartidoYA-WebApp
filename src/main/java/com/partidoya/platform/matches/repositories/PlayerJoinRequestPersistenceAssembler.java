package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.PlayerJoinRequest;
import com.partidoya.platform.matches.models.JoinRequestStatus;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.repositories.PlayerJoinRequestPersistenceEntity;

public final class PlayerJoinRequestPersistenceAssembler {
    private PlayerJoinRequestPersistenceAssembler() {
    }

    public static PlayerJoinRequest toDomainFromPersistence(PlayerJoinRequestPersistenceEntity entity) {
        if (entity == null) return null;
        return new PlayerJoinRequest(
                entity.getId(),
                new MatchId(entity.getMatchId()),
                new UserId(entity.getPlayerId()),
                entity.getProofStorageKey(),
                entity.getOriginalFileName(),
                entity.getContentType(),
                entity.getAmount(),
                JoinRequestStatus.valueOf(entity.getStatus()),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy() != null ? new UserId(entity.getReviewedBy()) : null,
                entity.getRejectionReason());
    }

    public static PlayerJoinRequestPersistenceEntity toPersistenceFromDomain(PlayerJoinRequest request) {
        if (request == null) return null;
        var entity = new PlayerJoinRequestPersistenceEntity();
        if (request.getId() != null) entity.setId(request.getId());
        entity.setMatchId(request.getMatchId().value());
        entity.setPlayerId(request.getPlayerId().value());
        entity.setProofStorageKey(request.getProofStorageKey());
        entity.setOriginalFileName(request.getOriginalFileName());
        entity.setContentType(request.getContentType());
        entity.setAmount(request.getAmount());
        entity.setStatus(request.getStatus().name());
        entity.setSubmittedAt(request.getSubmittedAt());
        entity.setReviewedAt(request.getReviewedAt());
        entity.setReviewedBy(request.getReviewedBy() != null ? request.getReviewedBy().value() : null);
        entity.setRejectionReason(request.getRejectionReason());
        return entity;
    }
}
