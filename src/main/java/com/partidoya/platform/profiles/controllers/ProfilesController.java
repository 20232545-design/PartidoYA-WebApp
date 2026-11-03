package com.partidoya.platform.profiles.controllers;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.ProfileId;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.TimeSlot;
import com.partidoya.platform.profiles.dto.CreateProfileResource;
import com.partidoya.platform.profiles.dto.ProfileResource;
import com.partidoya.platform.profiles.dto.UpdateGamePreferencesResource;
import com.partidoya.platform.profiles.dto.UpdatePrimarySportResource;
import com.partidoya.platform.profiles.dto.UpdateProfileResource;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.dto.CreateProfileCommandFromResourceAssembler;
import com.partidoya.platform.profiles.dto.ProfileResourceFromEntityAssembler;
import com.partidoya.platform.profiles.services.ProfileService;
import com.partidoya.platform.auth.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/profiles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "User profile and sport preferences management")
public class ProfilesController {
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfilesController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ProfileResource> createProfile(@RequestBody CreateProfileResource resource) {
        currentUserService.requireSelf(resource.userId());
        var command = CreateProfileCommandFromResourceAssembler.toCommandFromResource(resource);
        var profile = profileService.create(command);
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResource> getProfileById(@PathVariable Long profileId) {
        var profile = profileService.findById(new ProfileId(profileId));
        profile.ifPresent(value -> currentUserService.requireSelf(value.getUserId().value()));
        return profile
                .map(ProfileResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResource> getProfileByUserId(@PathVariable Long userId) {
        var profile = profileService.findByUserId(currentUserService.requireSelf(userId));
        return profile
                .map(ProfileResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/user/{userId}/primary-sport")
    public ResponseEntity<ProfileResource> updatePrimarySport(@PathVariable Long userId, @RequestBody UpdatePrimarySportResource resource) {
        var profile = profileService.updatePrimarySport(currentUserService.requireSelf(userId), new SportName(resource.primarySport()));
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/user/{userId}/game-preferences")
    public ResponseEntity<ProfileResource> updateGamePreferences(@PathVariable Long userId, @RequestBody UpdateGamePreferencesResource resource) {
        Set<TimeSlot> availability = resource.availability() == null
                ? Set.of()
                : resource.availability().stream().map(TimeSlot::valueOf).collect(Collectors.toSet());
        var profile = profileService.updateGamePreferences(
                currentUserService.requireSelf(userId),
                SkillLevel.valueOf(resource.skillLevel()),
                availability);
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/user/{userId}")
    public ResponseEntity<ProfileResource> updateProfile(@PathVariable Long userId, @RequestBody UpdateProfileResource resource) {
        var availability = resource.availability() == null
                ? Set.<TimeSlot>of()
                : resource.availability().stream().map(value -> TimeSlot.valueOf(value.toUpperCase())).collect(Collectors.toSet());
        var profile = profileService.updateDetails(
                currentUserService.requireSelf(userId),
                optionalPhone(resource.phoneNumber()),
                resource.district(),
                optionalSport(resource.primarySport()),
                optionalSkillLevel(resource.skillLevel()),
                availability,
                resource.businessName(),
                optionalPhone(resource.businessPhone()),
                resource.operationDistrict());
        return ResponseEntity.ok(ProfileResourceFromEntityAssembler.toResourceFromEntity(profile));
    }

    private PhoneNumber optionalPhone(String value) {
        return value == null || value.isBlank() ? null : new PhoneNumber(value.trim());
    }

    private SportName optionalSport(String value) {
        return value == null || value.isBlank() ? null : new SportName(value.trim());
    }

    private SkillLevel optionalSkillLevel(String value) {
        return value == null || value.isBlank() ? null : SkillLevel.valueOf(value.trim().toUpperCase());
    }
}
