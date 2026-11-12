package com.partidoya.platform.moderation.models;

/** Reporte enriquecido con los nombres que necesita la pantalla de moderación. */
public record PlayerReportView(
        PlayerReport report,
        String reporterName,
        String reportedPlayerName,
        String matchTitle) {
}
