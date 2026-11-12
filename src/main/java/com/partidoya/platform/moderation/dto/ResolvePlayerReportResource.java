package com.partidoya.platform.moderation.dto;

import com.partidoya.platform.moderation.models.PlayerReportStatus;

/** {@code outcome} debe ser ACTION_TAKEN o DISMISSED. */
public record ResolvePlayerReportResource(PlayerReportStatus outcome, String note) {
}
