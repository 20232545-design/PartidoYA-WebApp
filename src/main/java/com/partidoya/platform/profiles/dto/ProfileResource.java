package com.partidoya.platform.profiles.dto;

import java.util.List;

public record ProfileResource(
        Long id,
        Long userId,
        String phoneNumber,
        String avatarUrl,
        String primarySport,
        String skillLevel,
        List<String> availability,
        List<SportPreferenceResource> sportPreferences,
        String district,
        String businessName,
        String businessPhone,
        String operationDistrict) {
}
