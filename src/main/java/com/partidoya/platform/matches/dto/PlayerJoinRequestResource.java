package com.partidoya.platform.matches.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlayerJoinRequestResource(
        Long id,
        Long matchId,
        Long playerId,
        /** Nombre visible del jugador; evita que el organizador consulte la cuenta de otro usuario. */
        String playerName,
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
