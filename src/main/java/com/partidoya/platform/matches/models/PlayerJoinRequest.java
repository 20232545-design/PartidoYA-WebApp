package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.JoinRequestStatus;
import com.partidoya.platform.matches.models.MatchId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class PlayerJoinRequest {
    private Long id;
    private MatchId matchId;
    private UserId playerId;
    private String proofStorageKey;
    private String originalFileName;
    private String contentType;
    private BigDecimal amount;
    private JoinRequestStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private UserId reviewedBy;
    private String rejectionReason;

    protected PlayerJoinRequest() {
    }

    public PlayerJoinRequest(MatchId matchId, UserId playerId, String proofStorageKey, String originalFileName,
                             String contentType, BigDecimal amount) {
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        replaceProof(proofStorageKey, originalFileName, contentType, amount);
    }

    public PlayerJoinRequest(Long id, MatchId matchId, UserId playerId, String proofStorageKey,
                             String originalFileName, String contentType, BigDecimal amount,
                             JoinRequestStatus status, LocalDateTime submittedAt, LocalDateTime reviewedAt,
                             UserId reviewedBy, String rejectionReason) {
        this.matchId = Objects.requireNonNull(matchId, "matchId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.id = id;
        this.proofStorageKey = proofStorageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.amount = amount;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.rejectionReason = rejectionReason;
    }

    public void replaceProof(String proofStorageKey, String originalFileName, String contentType, BigDecimal amount) {
        if (proofStorageKey == null || proofStorageKey.isBlank()) throw new IllegalArgumentException("proof is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be greater than zero");
        if (status != null && status != JoinRequestStatus.REJECTED) {
            throw new IllegalStateException("Only rejected proofs can be replaced");
        }
        this.proofStorageKey = proofStorageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.amount = amount;
        this.status = JoinRequestStatus.PENDING_PAYMENT_VERIFICATION;
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = null;
        this.reviewedBy = null;
        this.rejectionReason = null;
    }

    public void approve(UserId reviewerId) {
        if (status != JoinRequestStatus.PENDING_PAYMENT_VERIFICATION) {
            throw new IllegalStateException("Only pending proofs can be approved");
        }
        if (playerId.value().equals(reviewerId.value())) {
            throw new IllegalStateException("A player cannot approve their own proof");
        }
        this.status = JoinRequestStatus.CONFIRMED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerId;
    }

    public void reject(UserId reviewerId, String reason) {
        if (status != JoinRequestStatus.PENDING_PAYMENT_VERIFICATION) {
            throw new IllegalStateException("Only pending proofs can be rejected");
        }
        if (playerId.value().equals(reviewerId.value())) {
            throw new IllegalStateException("A player cannot reject their own proof");
        }
        this.status = JoinRequestStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewerId;
        this.rejectionReason = reason == null || reason.isBlank() ? null : reason.trim();
    }
}
