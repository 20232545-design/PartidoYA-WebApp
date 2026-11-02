package com.partidoya.platform.courts.models;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class Court {
    private CourtId id;
    private UserId ownerId;
    private String name;
    private String complexName;
    private String description;
    private String address;
    private String district;
    private Double latitude;
    private Double longitude;
    private BigDecimal pricePerHour;
    private boolean active;
    private boolean published;
    private boolean availableForReservations;
    private List<String> sports;
    private List<String> schedules;
    private String mainImageUrl;
    private List<String> imageUrls;
    private List<String> services;
    private List<String> features;

    protected Court() {
    }

    public Court(UserId ownerId, String name, String complexName, String description, String address,
                 String district, Double latitude, Double longitude, BigDecimal pricePerHour,
                 List<String> sports, List<String> schedules, String mainImageUrl, List<String> imageUrls,
                 List<String> services, List<String> features) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        updateDetails(name, complexName, description, address, district, latitude, longitude, pricePerHour,
                sports, schedules, mainImageUrl, imageUrls, services, features);
        this.active = true;
        this.published = false;
        this.availableForReservations = true;
    }

    public Court(CourtId id, UserId ownerId, String name, String complexName, String description, String address,
                 String district, Double latitude, Double longitude, BigDecimal pricePerHour, boolean active,
                 boolean published, boolean availableForReservations, List<String> sports, List<String> schedules,
                 String mainImageUrl, List<String> imageUrls, List<String> services, List<String> features) {
        this(ownerId, name, complexName, description, address, district, latitude, longitude, pricePerHour,
                sports, schedules, mainImageUrl, imageUrls, services, features);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.active = active;
        this.published = published;
        this.availableForReservations = availableForReservations;
    }

    public void updateDetails(String name, String complexName, String description, String address,
                              String district, Double latitude, Double longitude, BigDecimal pricePerHour,
                              List<String> sports, List<String> schedules, String mainImageUrl,
                              List<String> imageUrls, List<String> services, List<String> features) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("court name is required");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("court address is required");
        if (district == null || district.isBlank()) throw new IllegalArgumentException("court district is required");
        if (pricePerHour == null || pricePerHour.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("court price must be zero or greater");
        }
        if (sports == null || sports.isEmpty()) throw new IllegalArgumentException("court sports are required");
        if (schedules == null || schedules.isEmpty()) throw new IllegalArgumentException("court schedules are required");
        this.name = name.trim();
        this.complexName = normalize(complexName);
        this.description = normalize(description);
        this.address = address.trim();
        this.district = district.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.pricePerHour = pricePerHour;
        this.sports = cleanList(sports);
        this.schedules = cleanList(schedules);
        this.mainImageUrl = normalize(mainImageUrl);
        this.imageUrls = cleanList(imageUrls);
        this.services = cleanList(services);
        this.features = cleanList(features);
    }

    public void publish() {
        if (!active) throw new IllegalStateException("Inactive courts cannot be published");
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }

    public boolean canReceiveReservations() {
        return active && published && availableForReservations;
    }

    public boolean supportsSchedule(String schedule) {
        return schedules.stream().anyMatch(s -> s.equalsIgnoreCase(schedule));
    }

    public List<String> getSports() {
        return Collections.unmodifiableList(sports);
    }

    public List<String> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    public List<String> getImageUrls() {
        return Collections.unmodifiableList(imageUrls);
    }

    public List<String> getServices() {
        return Collections.unmodifiableList(services);
    }

    public List<String> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<String> cleanList(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
