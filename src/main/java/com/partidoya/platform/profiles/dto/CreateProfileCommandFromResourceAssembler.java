package com.partidoya.platform.profiles.dto;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.CreateProfileCommand;
import com.partidoya.platform.profiles.models.AvatarUrl;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.SportPreference;
import com.partidoya.platform.profiles.dto.CreateProfileResource;

import java.util.List;

public class CreateProfileCommandFromResourceAssembler {
    public static CreateProfileCommand toCommandFromResource(CreateProfileResource resource) {
        var preferences = resource.sportPreferences() != null
                ? resource.sportPreferences().stream()
                    .map(sp -> new SportPreference(
                            new SportName(sp.sportName()),
                            SkillLevel.valueOf(sp.skillLevel().toUpperCase())))
                    .toList()
                : List.<SportPreference>of();

        return new CreateProfileCommand(
                new UserId(resource.userId()),
                new PhoneNumber(resource.phoneNumber()),
                new AvatarUrl(resource.avatarUrl()),
                resource.primarySport() != null ? new SportName(resource.primarySport()) : null,
                preferences);
    }
}
