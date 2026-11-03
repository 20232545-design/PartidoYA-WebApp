package com.partidoya.platform.profiles.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.CreateProfileCommand;
import com.partidoya.platform.profiles.models.AvatarUrl;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.models.ProfileId;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.SportPreference;
import com.partidoya.platform.profiles.models.TimeSlot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
public class Profile {
    private ProfileId id;
    private UserId userId;
    private PhoneNumber phoneNumber;
    private AvatarUrl avatarUrl;
    private SportName primarySport;
    private SkillLevel skillLevel;
    private Set<TimeSlot> availability;
    private List<SportPreference> sportPreferences;
    private String district;
    private String businessName;
    private PhoneNumber businessPhone;
    private String operationDistrict;

    protected Profile() {
    }

    public Profile(UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport, List<SportPreference> sportPreferences) {
        this(userId, phoneNumber, avatarUrl, primarySport, null, null, sportPreferences);
    }

    public Profile(UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport,
                    SkillLevel skillLevel, Set<TimeSlot> availability, List<SportPreference> sportPreferences) {
        this(userId, phoneNumber, avatarUrl, primarySport, skillLevel, availability, sportPreferences,
                null, null, null, null);
    }

    public Profile(UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport,
                   SkillLevel skillLevel, Set<TimeSlot> availability, List<SportPreference> sportPreferences,
                   String district, String businessName, PhoneNumber businessPhone, String operationDistrict) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.primarySport = primarySport;
        this.skillLevel = skillLevel;
        this.availability = availability != null ? new HashSet<>(availability) : new HashSet<>();
        this.sportPreferences = sportPreferences != null ? new ArrayList<>(sportPreferences) : new ArrayList<>();
        this.district = normalize(district);
        this.businessName = normalize(businessName);
        this.businessPhone = businessPhone;
        this.operationDistrict = normalize(operationDistrict);
    }

    public Profile(CreateProfileCommand command) {
        this(command.userId(), command.phoneNumber(), command.avatarUrl(), command.primarySport(), command.sportPreferences());
    }

    public Profile(ProfileId id, UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport, List<SportPreference> sportPreferences) {
        this(userId, phoneNumber, avatarUrl, primarySport, sportPreferences);
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public Profile(ProfileId id, UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport,
                    SkillLevel skillLevel, Set<TimeSlot> availability, List<SportPreference> sportPreferences) {
        this(userId, phoneNumber, avatarUrl, primarySport, skillLevel, availability, sportPreferences);
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public Profile(ProfileId id, UserId userId, PhoneNumber phoneNumber, AvatarUrl avatarUrl, SportName primarySport,
                   SkillLevel skillLevel, Set<TimeSlot> availability, List<SportPreference> sportPreferences,
                   String district, String businessName, PhoneNumber businessPhone, String operationDistrict) {
        this(userId, phoneNumber, avatarUrl, primarySport, skillLevel, availability, sportPreferences,
                district, businessName, businessPhone, operationDistrict);
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public List<SportPreference> getSportPreferences() {
        return Collections.unmodifiableList(sportPreferences);
    }

    public Set<TimeSlot> getAvailability() {
        return Collections.unmodifiableSet(availability);
    }

    public void updatePrimarySport(SportName primarySport) {
        this.primarySport = Objects.requireNonNull(primarySport, "primarySport must not be null");
    }

    public void updateSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = Objects.requireNonNull(skillLevel, "skillLevel must not be null");
    }

    public void updateAvailability(Set<TimeSlot> availability) {
        this.availability = availability != null ? new HashSet<>(availability) : new HashSet<>();
    }

    public void updateDetails(PhoneNumber phoneNumber, String district, SportName primarySport,
                              SkillLevel skillLevel, Set<TimeSlot> availability, String businessName,
                              PhoneNumber businessPhone, String operationDistrict) {
        this.phoneNumber = phoneNumber;
        this.district = normalize(district);
        this.primarySport = primarySport;
        this.skillLevel = skillLevel;
        this.availability = availability == null ? new HashSet<>() : new HashSet<>(availability);
        this.businessName = normalize(businessName);
        this.businessPhone = businessPhone;
        this.operationDistrict = normalize(operationDistrict);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
