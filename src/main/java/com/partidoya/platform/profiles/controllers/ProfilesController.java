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
import com.partidoya.platform.profiles.dto.CreateProfileCommandFromResourceAssembler;
import com.partidoya.platform.profiles.dto.ProfileResourceFromEntityAssembler;
import com.partidoya.platform.profiles.services.ProfileService;
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

    public ProfilesController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<ProfileResource> createProfile(@RequestBody CreateProfileResource resource) {
        var command = CreateProfileCommandFromResourceAssembler.toCommandFromResource(resource);
        var profile = profileService.create(command);
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResource> getProfileById(@PathVariable Long profileId) {
        var profile = profileService.findById(new ProfileId(profileId));
        return profile
                .map(ProfileResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResource> getProfileByUserId(@PathVariable Long userId) {
        var profile = profileService.findByUserId(new UserId(userId));
        return profile
                .map(ProfileResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/user/{userId}/primary-sport")
    public ResponseEntity<ProfileResource> updatePrimarySport(@PathVariable Long userId, @RequestBody UpdatePrimarySportResource resource) {
        var profile = profileService.updatePrimarySport(new UserId(userId), new SportName(resource.primarySport()));
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/user/{userId}/game-preferences")
    public ResponseEntity<ProfileResource> updateGamePreferences(@PathVariable Long userId, @RequestBody UpdateGamePreferencesResource resource) {
        Set<TimeSlot> availability = resource.availability() == null
                ? Set.of()
                : resource.availability().stream().map(TimeSlot::valueOf).collect(Collectors.toSet());
        var profile = profileService.updateGamePreferences(
                new UserId(userId),
                SkillLevel.valueOf(resource.skillLevel()),
                availability);
        var body = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile);
        return ResponseEntity.ok(body);
    }
}
