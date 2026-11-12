package com.partidoya.platform.moderation.dto;

/**
 * Respuesta para quien envía el reporte: confirma la recepción sin exponer datos de moderación.
 */
public record SubmittedPlayerReportResource(Long id, Long matchId, Long reportedPlayerId, String status,
                                            String submittedAt) {
}
