package com.partidoya.platform.auth.dto;

public record RegisterUserResource(String email, String fullName, String password, String role) {
}
