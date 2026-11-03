package com.partidoya.platform.users.dto;

public record ChangePasswordResource(String currentPassword, String newPassword) {
}
