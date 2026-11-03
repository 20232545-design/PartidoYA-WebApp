package com.partidoya.platform.auth.services;

import com.partidoya.platform.auth.models.RegisterUserCommand;
import com.partidoya.platform.auth.security.PasswordHasher;
import com.partidoya.platform.profiles.models.Profile;
import com.partidoya.platform.profiles.repositories.ProfileRepository;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.models.UserStatus;
import com.partidoya.platform.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private ProfileRepository profileRepository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        profileRepository = mock(ProfileRepository.class);
        service = new AuthService(userRepository, passwordHasher, profileRepository);
        when(passwordHasher.hash(any())).thenReturn("hashed-password");
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersPlayerBasicWithoutPaidPrivileges() {
        var command = playerCommand(Plan.JUGADOR_BASICO);
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(savedUser(command, 10L));

        var user = service.register(command);

        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_BASICO);
        assertThat(user.isLegacyPlanAccess()).isFalse();
        var profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDistrict()).isEqualTo("Miraflores");
        assertThat(profileCaptor.getValue().getPrimarySport().value()).isEqualTo("Fútbol");
    }

    @Test
    void registersPaidPlayerAsBasicUntilProviderConfirmation() {
        var command = playerCommand(Plan.JUGADOR_PLUS);
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(savedUser(command, 11L));

        var user = service.register(command);

        assertThat(user.getPlan()).isEqualTo(Plan.JUGADOR_BASICO);
    }

    @Test
    void registersCourtAdministratorWithoutCommercialPlan() {
        var command = new RegisterUserCommand(
                new EmailAddress("cancha@test.com"),
                new FullName("Ana Cancha"),
                "Ana",
                "Cancha",
                new RawPassword("Segura#1234"),
                Role.ADMIN_CANCHA,
                Plan.CANCHA_BUSINESS,
                "+51987654321",
                "San Isidro",
                null,
                null,
                List.of(),
                "Complejo Norte",
                "+51912345678",
                "San Isidro",
                true,
                true,
                false);
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(savedUser(command, 12L));

        var user = service.register(command);

        assertThat(user.getPlan()).isEqualTo(Plan.CANCHA_SIN_PLAN);
        var profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getBusinessName()).isEqualTo("Complejo Norte");
    }

    private RegisterUserCommand playerCommand(Plan requestedPlan) {
        return new RegisterUserCommand(
                new EmailAddress("jugador@test.com"),
                new FullName("Luis Jugador"),
                "Luis",
                "Jugador",
                new RawPassword("Segura#1234"),
                Role.JUGADOR,
                requestedPlan,
                "+51987654321",
                "Miraflores",
                "Fútbol",
                "INTERMEDIATE",
                List.of("EVENING"),
                null,
                null,
                null,
                true,
                true,
                true);
    }

    private User savedUser(RegisterUserCommand command, Long id) {
        return new User(
                new UserId(id),
                command.email(),
                command.fullName(),
                new HashedPassword("hashed-password"),
                false,
                command.role(),
                Plan.defaultForRole(command.role()),
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                command.firstName(),
                command.lastName(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                command.marketingConsent(),
                false);
    }
}
