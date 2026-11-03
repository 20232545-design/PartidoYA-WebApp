package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
public class CourtPersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "complex_name", length = 120)
    private String complexName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 240)
    private String address;

    @Column(nullable = false, length = 80)
    private String district;

    private Double latitude;
    private Double longitude;

    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "google_place_name", length = 180)
    private String googlePlaceName;

    @Column(name = "surface_type", length = 60)
    private String surfaceType;

    private Integer capacity;

    @Column(nullable = false)
    private boolean covered;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "available_for_reservations", nullable = false)
    private boolean availableForReservations;

    @Column(name = "main_image_url")
    private String mainImageUrl;

    @ElementCollection
    @CollectionTable(name = "court_sports", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "sport", nullable = false, length = 80)
    private List<String> sports = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "court_schedules", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "schedule", nullable = false, length = 30)
    private List<String> schedules = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "court_images", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "court_services", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "service", length = 100)
    private List<String> services = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "court_features", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "feature", length = 100)
    private List<String> features = new ArrayList<>();
}
