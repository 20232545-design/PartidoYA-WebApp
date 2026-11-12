package com.partidoya.platform.moderation.dto;

import com.partidoya.platform.moderation.models.ReportReason;

public record SubmitPlayerReportResource(Long reportedPlayerId, ReportReason reason, String details) {
}
