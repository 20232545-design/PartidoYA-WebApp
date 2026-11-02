package com.partidoya.platform.users.models;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.models.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAdministrativeStateTest {
    @Test
    void suspendsAndReactivatesUserWithAdministrativeMetadata() {
        var user = user();
        var adminId = new UserId(99L);

        user.suspend(adminId, "Conducta indebida", LocalDate.now().plusDays(7));

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getSuspensionReason()).isEqualTo("Conducta indebida");
        assertThat(user.getLastAdministrativeActionBy()).isEqualTo(adminId);

        user.reactivate(adminId, "Apelacion aceptada");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSuspensionReason()).isEqualTo("Apelacion aceptada");
    }

    @Test
    void requiresReasonToSuspendUser() {
        assertThatThrownBy(() -> user().suspend(new UserId(99L), " ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static User user() {
        return new User(new EmailAddress("user@test.com"), new FullName("Usuario Test"),
                new HashedPassword("hash"), Role.JUGADOR);
    }
}
