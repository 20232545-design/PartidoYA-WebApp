package com.partidoya.platform.courts.services;

import java.math.BigDecimal;
import java.util.List;

public record CourtDetails(
        String name,
        String complexName,
        String description,
        String address,
        String district,
        Double latitude,
        Double longitude,
        BigDecimal pricePerHour,
        List<String> sports,
        List<String> schedules,
        String mainImageUrl,
        List<String> imageUrls,
        List<String> services,
        List<String> features
) {
}
