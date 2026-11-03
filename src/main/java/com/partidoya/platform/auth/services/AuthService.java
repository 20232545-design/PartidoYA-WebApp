package com.partidoya.platform.auth.services;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.auth.models.AuthenticateUserCommand;
import com.partidoya.platform.auth.models.RegisterUserCommand;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.auth.security.PasswordHasher;
import com.partidoya.platform.profiles.models.AvatarUrl;
import com.partidoya.platform.profiles.models.PhoneNumber;
import com.partidoya.platform.profiles.models.Profile;
import com.partidoya.platform.profiles.models.SkillLevel;
import com.partidoya.platform.profiles.models.SportName;
import com.partidoya.platform.profiles.models.SportPreference;
import com.partidoya.platform.profiles.models.TimeSlot;
import com.partidoya.platform.profiles.repositories.ProfileRepository;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ProfileRepository profileRepository;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.profileRepository = profileRepository;
    }

    public User register(RegisterUserCommand request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException(
                    "User",
                    "Email '%s' is already registered".formatted(request.email().value()));
        }
        validatePasswordStrength(request.password().value());
        var password = new HashedPassword(passwordHasher.hash(request.password().value()));
        var user = new User(request.email(), request.fullName(), password, request.role());
        user.completeRegistration(request.firstName(), request.lastName(), request.termsAccepted(),
                request.privacyAccepted(), request.marketingConsent());
        var saved = userRepository.save(user);
        profileRepository.save(createProfile(saved, request));
        return saved;
    }

    @Transactional(readOnly = true)
    public User authenticate(AuthenticateUserCommand request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email().value()));
        if (!passwordHasher.matches(request.password().value(), user.getPassword().value())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!user.isActive()) {
            throw new ForbiddenActionException("Account is suspended");
        }
        return user;
    }

    private Profile createProfile(User user, RegisterUserCommand request) {
        var phone = optionalPhone(request.phone());
        if (user.getRole() == Role.JUGADOR) {
            var sport = optionalSport(request.primarySport());
            var level = optionalSkillLevel(request.skillLevel());
            var preferences = sport == null || level == null
                    ? java.util.List.<SportPreference>of()
                    : java.util.List.of(new SportPreference(sport, level));
            var availability = request.availability().stream()
                    .map(value -> TimeSlot.valueOf(value.trim().toUpperCase()))
                    .collect(java.util.stream.Collectors.toSet());
            return new Profile(user.getId(), phone, new AvatarUrl(null), sport, level, availability, preferences,
                    request.district(), null, null, null);
        }
        if (request.businessName() == null || request.businessName().isBlank()
                || request.operationDistrict() == null || request.operationDistrict().isBlank()) {
            throw new IllegalArgumentException("Business name and operation district are required");
        }
        return new Profile(user.getId(), phone, new AvatarUrl(null), null, null, java.util.Set.of(), java.util.List.of(),
                request.district(), request.businessName(), optionalPhone(request.businessPhone()), request.operationDistrict());
    }

    private static PhoneNumber optionalPhone(String value) {
        return value == null || value.isBlank() ? null : new PhoneNumber(value.trim());
    }

    private static SportName optionalSport(String value) {
        return value == null || value.isBlank() ? null : new SportName(value.trim());
    }

    private static SkillLevel optionalSkillLevel(String value) {
        return value == null || value.isBlank() ? null : SkillLevel.valueOf(value.trim().toUpperCase());
    }

    private static void validatePasswordStrength(String password) {
        if (password.length() < 10
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least 10 characters, uppercase, lowercase, number and symbol");
        }
    }
}
