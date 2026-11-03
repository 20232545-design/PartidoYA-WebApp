package com.partidoya.platform.auth.dto;

import java.util.List;

public record RegisterUserResource(
        String email,
        String firstName,
        String lastName,
        String password,
        String role,
        String planCode,
        String phone,
        String district,
        String primarySport,
        String skillLevel,
        List<String> availability,
        String businessName,
        String businessPhone,
        String operationDistrict,
        boolean termsAccepted,
        boolean privacyAccepted,
        boolean marketingConsent) {
}
