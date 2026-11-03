package com.partidoya.platform.profiles.dto;

import java.util.List;

public record UpdateProfileResource(
        String phoneNumber,
        String district,
        String primarySport,
        String skillLevel,
        List<String> availability,
        String businessName,
        String businessPhone,
        String operationDistrict) {
}
