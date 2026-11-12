package com.partidoya.platform.moderation.dto;

import java.util.List;

public record PlayerReportPageResource(List<PlayerReportResource> content, long totalElements, int page, int size) {
}
