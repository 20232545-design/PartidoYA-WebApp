package com.partidoya.platform.courts.models;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.users.models.UserId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private String googlePlaceId;
    private String googlePlaceName;
    private String surfaceType;
    private Integer capacity;
    private boolean covered;

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
        if (latitude == null || longitude == null || surfaceType == null || capacity == null || capacity <= 0) {
            throw new IllegalStateException("Completa ubicación, superficie y capacidad antes de publicar");
        }
        if (latitude < -18.5 || latitude > 0.5 || longitude < -81.5 || longitude > -68.5) {
            throw new IllegalStateException("La ubicación debe encontrarse dentro del Perú");
        }
        if (mainImageUrl == null && imageUrls.isEmpty()) {
            throw new IllegalStateException("Agrega al menos una imagen antes de publicar");
        }
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }

    public boolean canReceiveReservations() {
        return active && published && availableForReservations;
    }

    /**
     * Turnos reservables de la cancha en una fecha ({@code "08:00-09:00"}), ordenados y sin repetir.
     * Interpreta tanto turnos fijos como plantillas semanales; ignora entradas con formato desconocido.
     */
    public List<String> slotsOn(LocalDate date) {
        return schedules.stream()
                .map(CourtSchedule::parse)
                .flatMap(Optional::stream)
                .flatMap(schedule -> schedule.slotsOn(date).stream())
                .map(CourtSchedule.TimeSlot::label)
                .distinct()
                .sorted()
                .toList();
    }

    public boolean supportsSlot(LocalDate date, LocalTime start, LocalTime end) {
        return slotsOn(date).contains(start + "-" + end);
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

    public void updateVenueMetadata(String googlePlaceId, String googlePlaceName, String surfaceType,
                                    Integer capacity, boolean covered) {
        if (capacity != null && capacity <= 0) throw new IllegalArgumentException("court capacity must be greater than zero");
        this.googlePlaceId = normalize(googlePlaceId);
        this.googlePlaceName = normalize(googlePlaceName);
        this.surfaceType = normalize(surfaceType);
        this.capacity = capacity;
        this.covered = covered;
    }

    public void addImage(String secureUrl, boolean primary) {
        var url = normalize(secureUrl);
        if (url == null) throw new IllegalArgumentException("image url is required");
        var images = new ArrayList<>(this.imageUrls);
        if (!images.contains(url)) images.add(url);
        this.imageUrls = images;
        if (primary || this.mainImageUrl == null) this.mainImageUrl = url;
    }

    public void removeImage(String secureUrl) {
        var images = new ArrayList<>(this.imageUrls);
        images.remove(secureUrl);
        this.imageUrls = images;
        if (Objects.equals(this.mainImageUrl, secureUrl)) {
            this.mainImageUrl = images.isEmpty() ? null : images.getFirst();
        }
    }

    public void setPrimaryImage(String secureUrl) {
        var url = normalize(secureUrl);
        if (url == null || !imageUrls.contains(url)) throw new IllegalArgumentException("La imagen no pertenece a la cancha");
        this.mainImageUrl = url;
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
