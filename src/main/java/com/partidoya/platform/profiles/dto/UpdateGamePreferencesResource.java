package com.partidoya.platform.profiles.dto;

import java.util.List;

public record UpdateGamePreferencesResource(String skillLevel, List<String> availability) {
}
