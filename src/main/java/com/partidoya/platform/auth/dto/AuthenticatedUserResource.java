package com.partidoya.platform.auth.dto;

public record AuthenticatedUserResource(String token, Long userId, String email, String fullName, String role, String plan) {
}
