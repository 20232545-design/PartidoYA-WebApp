package com.partidoya.platform.profiles.models;

import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileRoleSeparationTest {
    @Test
    void commercialUpdatePreservesPlayerInformation() {
        var profile = profile();

        profile.updateCourtAdminDetails(new PhoneNumber("987654321"), "Surco", "Arena Central",
                new PhoneNumber("912345678"), "Lima Sur");

        assertThat(profile.getPrimarySport().value()).isEqualTo("Fútbol");
        assertThat(profile.getSkillLevel()).isEqualTo(SkillLevel.INTERMEDIATE);
        assertThat(profile.getAvailability()).containsExactly(TimeSlot.EVENING);
        assertThat(profile.getSportPreferences()).hasSize(1);
        assertThat(profile.getBusinessName()).isEqualTo("Arena Central");
    }

    @Test
    void playerUpdatePreservesCommercialInformation() {
        var profile = profile();
        profile.updateCourtAdminDetails(null, "Surco", "Arena Central", null, "Lima Sur");

        profile.updatePlayerDetails(new PhoneNumber("987654321"), "Miraflores", new SportName("Vóley"),
                SkillLevel.ADVANCED, Set.of(TimeSlot.MORNING), List.of("Vóley", "Tenis"));

        assertThat(profile.getBusinessName()).isEqualTo("Arena Central");
        assertThat(profile.getOperationDistrict()).isEqualTo("Lima Sur");
        assertThat(profile.getPrimarySport().value()).isEqualTo("Vóley");
        assertThat(profile.getSportPreferences()).extracting(item -> item.sportName().value())
                .containsExactly("Vóley", "Tenis");
    }

    private Profile profile() {
        return new Profile(new UserId(1L), null, null, new SportName("Fútbol"), SkillLevel.INTERMEDIATE,
                Set.of(TimeSlot.EVENING), List.of(new SportPreference(new SportName("Fútbol"), SkillLevel.INTERMEDIATE)));
    }
}
