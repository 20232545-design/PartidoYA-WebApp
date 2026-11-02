package com.partidoya.platform.profiles.repositories;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.profiles.models.Profile;
import com.partidoya.platform.profiles.models.ProfileId;

import java.util.Optional;

public interface ProfileRepository {
    Optional<Profile> findById(ProfileId id);
    Optional<Profile> findByUserId(UserId userId);
    Profile save(Profile profile);
    boolean existsByUserId(UserId userId);
}
