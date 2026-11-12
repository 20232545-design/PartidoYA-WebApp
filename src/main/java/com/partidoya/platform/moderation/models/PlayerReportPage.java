package com.partidoya.platform.moderation.models;

import java.util.List;

public record PlayerReportPage(List<PlayerReportView> content, long totalElements, int page, int size) {
}
