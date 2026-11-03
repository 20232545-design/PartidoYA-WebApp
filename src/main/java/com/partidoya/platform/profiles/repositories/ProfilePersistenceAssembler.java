package com.partidoya.platform.profiles.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.Profile;
import com.partidoya.platform.profiles.models.AvatarUrl;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.models.ProfileId;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.SportPreference;
import com.partidoya.platform.profiles.models.TimeSlot;
import com.partidoya.platform.profiles.repositories.ProfilePersistenceEntity;
import com.partidoya.platform.profiles.repositories.SportPreferencePersistenceEntity;

import java.util.ArrayList;
import java.util.stream.Collectors;

public final class ProfilePersistenceAssembler {

    private ProfilePersistenceAssembler() {
    }

    public static Profile toDomainFromPersistence(ProfilePersistenceEntity entity) {
        if (entity == null) return null;
        var preferences = entity.getSportPreferences().stream()
                .map(sp -> new SportPreference(
                        new SportName(sp.getSportName()),
                        SkillLevel.valueOf(sp.getSkillLevel())))
                .toList();
        var availability = entity.getAvailability().stream()
                .map(TimeSlot::valueOf)
                .collect(Collectors.toSet());
        return new Profile(
                new ProfileId(entity.getId()),
                new UserId(entity.getUserId()),
                new PhoneNumber(entity.getPhoneNumber()),
                new AvatarUrl(entity.getAvatarUrl()),
                entity.getPrimarySport() != null ? new SportName(entity.getPrimarySport()) : null,
                entity.getSkillLevel() != null ? SkillLevel.valueOf(entity.getSkillLevel()) : null,
                availability,
                preferences,
                entity.getDistrict(),
                entity.getBusinessName(),
                entity.getBusinessPhone() == null ? null : new PhoneNumber(entity.getBusinessPhone()),
                entity.getOperationDistrict());
    }

    public static ProfilePersistenceEntity toPersistenceFromDomain(Profile profile) {
        if (profile == null) return null;
        var entity = new ProfilePersistenceEntity();
        if (profile.getId() != null) {
            entity.setId(profile.getId().value());
        }
        entity.setUserId(profile.getUserId().value());
        entity.setPhoneNumber(profile.getPhoneNumber() != null ? profile.getPhoneNumber().value() : null);
        entity.setAvatarUrl(profile.getAvatarUrl() != null ? profile.getAvatarUrl().value() : null);
        entity.setPrimarySport(profile.getPrimarySport() != null ? profile.getPrimarySport().value() : null);
        entity.setSkillLevel(profile.getSkillLevel() != null ? profile.getSkillLevel().name() : null);
        entity.setAvailability(profile.getAvailability().stream().map(Enum::name).collect(Collectors.toSet()));
        entity.setDistrict(profile.getDistrict());
        entity.setBusinessName(profile.getBusinessName());
        entity.setBusinessPhone(profile.getBusinessPhone() == null ? null : profile.getBusinessPhone().value());
        entity.setOperationDistrict(profile.getOperationDistrict());

        var preferences = new ArrayList<SportPreferencePersistenceEntity>();
        for (var sp : profile.getSportPreferences()) {
            var spEntity = new SportPreferencePersistenceEntity();
            spEntity.setSportName(sp.sportName().value());
            spEntity.setSkillLevel(sp.skillLevel().name());
            spEntity.setProfile(entity);
            preferences.add(spEntity);
        }
        entity.setSportPreferences(preferences);
        return entity;
    }
}
