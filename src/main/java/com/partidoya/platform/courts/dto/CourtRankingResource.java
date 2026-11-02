package com.partidoya.platform.courts.dto;

public record CourtRankingResource(Long courtId, String courtName, long reservations, double percentage) {
}
