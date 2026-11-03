package com.partidoya.platform.courts.dto;

public record CourtMediaResource(Long id, Long courtId, String source, String secureUrl,
                                 String googlePhotoName, String attribution, int displayOrder,
                                 boolean primaryAsset) {
}
