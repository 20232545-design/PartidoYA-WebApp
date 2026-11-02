package com.partidoya.platform.matches.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlayerJoinRequestResource(
        Long id,
        Long matchId,
        Long playerId,
        String proofUrl,
        String originalFileName,
        BigDecimal amount,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        Long reviewedBy,
        String rejectionReason
) {
}
