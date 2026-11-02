package com.partidoya.platform.profiles.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.AvatarUrl;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.SportPreference;

import java.util.List;
import java.util.Objects;

public record CreateProfileCommand(
        UserId userId,
        PhoneNumber phoneNumber,
        AvatarUrl avatarUrl,
        SportName primarySport,
        List<SportPreference> sportPreferences) {
    public CreateProfileCommand {
        Objects.requireNonNull(userId, "userId cannot be null");
        if (sportPreferences == null) {
            sportPreferences = List.of();
        }
    }
}
