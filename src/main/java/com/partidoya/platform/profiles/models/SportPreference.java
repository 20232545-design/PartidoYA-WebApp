package com.partidoya.platform.profiles.models;

import java.util.Objects;

public record SportPreference(SportName sportName, SkillLevel skillLevel) {
    public SportPreference {
        Objects.requireNonNull(sportName, "sportName must not be null");
        Objects.requireNonNull(skillLevel, "skillLevel must not be null");
    }
}
