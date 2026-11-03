package com.partidoya.platform.profiles.services;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.Profile;
import com.partidoya.platform.profiles.models.CreateProfileCommand;
import com.partidoya.platform.profiles.models.ProfileId;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.TimeSlot;
import com.partidoya.platform.profiles.repositories.ProfileRepository;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile create(CreateProfileCommand request) {
        if (profileRepository.existsByUserId(request.userId())) {
            throw new ResourceConflictException("Profile", "A profile already exists for this user");
        }
        return profileRepository.save(new Profile(request));
    }

    public Profile updatePrimarySport(UserId userId, SportName primarySport) {
        var profile = findOrCreate(userId);
        profile.updatePrimarySport(primarySport);
        return profileRepository.save(profile);
    }

    public Profile updateGamePreferences(UserId userId, SkillLevel skillLevel, Set<TimeSlot> availability) {
        var profile = findOrCreate(userId);
        profile.updateSkillLevel(skillLevel);
        profile.updateAvailability(availability);
        return profileRepository.save(profile);
    }

    public Profile updateDetails(UserId userId, com.partidoya.platform.profiles.models.PhoneNumber phoneNumber,
                                 String district, SportName primarySport, SkillLevel skillLevel,
                                 Set<TimeSlot> availability, String businessName,
                                 com.partidoya.platform.profiles.models.PhoneNumber businessPhone,
                                 String operationDistrict) {
        var profile = findOrCreate(userId);
        profile.updateDetails(phoneNumber, district, primarySport, skillLevel, availability,
                businessName, businessPhone, operationDistrict);
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Optional<Profile> findById(ProfileId profileId) {
        return profileRepository.findById(profileId);
    }

    @Transactional(readOnly = true)
    public Optional<Profile> findByUserId(UserId userId) {
        return profileRepository.findByUserId(userId);
    }

    private Profile findOrCreate(UserId userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> new Profile(userId, null, null, null, List.of()));
    }
}
