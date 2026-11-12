package com.partidoya.platform.moderation.models;

public enum ReportReason {
    UNSPORTSMANLIKE_CONDUCT,
    HARASSMENT,
    NO_SHOW,
    FALSE_INFORMATION,
    /** Requiere detalles, porque el motivo por sí solo no le dice nada al moderador. */
    OTHER
}
