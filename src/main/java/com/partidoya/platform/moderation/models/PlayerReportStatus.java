package com.partidoya.platform.moderation.models;

public enum PlayerReportStatus {
    PENDING,
    /** Un administrador revisó el reporte y tomó medidas contra el jugador reportado. */
    ACTION_TAKEN,
    /** Un administrador revisó el reporte y concluyó que no amerita medidas. */
    DISMISSED;

    public boolean isResolution() {
        return this != PENDING;
    }
}
