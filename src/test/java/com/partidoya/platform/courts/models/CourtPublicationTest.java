package com.partidoya.platform.courts.models;

import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourtPublicationTest {
    @Test
    void publishesCompleteCourtInsideSupportedArea() {
        var court = court(-12.12, -77.03, "https://res.cloudinary.com/demo/image/upload/court.jpg");

        court.publish();

        assertThat(court.isPublished()).isTrue();
    }

    @Test
    void rejectsPublicationWithoutImageOrOutsidePeru() {
        assertThatThrownBy(() -> court(-12.12, -77.03, null).publish())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("imagen");
        assertThatThrownBy(() -> court(40.71, -74.00, "https://example.com/court.jpg").publish())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Perú");
    }

    private Court court(Double latitude, Double longitude, String imageUrl) {
        var court = new Court(new UserId(1L), "Cancha Central", null, null, "Av. Principal 100",
                "Miraflores", latitude, longitude, new BigDecimal("50.00"), List.of("Fútbol"),
                List.of("MONDAY|08:00|22:00|60"), imageUrl,
                imageUrl == null ? List.of() : List.of(imageUrl), List.of("Iluminación"), List.of());
        court.updateVenueMetadata("place-1", "Cancha Central", "Grass sintético", 12, false);
        return court;
    }
}
