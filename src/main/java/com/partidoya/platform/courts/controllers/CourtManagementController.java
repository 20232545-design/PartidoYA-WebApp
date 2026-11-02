package com.partidoya.platform.courts.controllers;

import com.partidoya.platform.courts.models.CourtAvailabilityId;
import com.partidoya.platform.courts.models.CourtAvailabilityType;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.dto.CourtAvailabilityResource;
import com.partidoya.platform.courts.dto.CourtReportResource;
import com.partidoya.platform.courts.dto.CourtResource;
import com.partidoya.platform.courts.dto.CreateCourtResource;
import com.partidoya.platform.courts.dto.ManagedReservationResource;
import com.partidoya.platform.courts.dto.PublishCourtResource;
import com.partidoya.platform.courts.dto.SaveCourtAvailabilityResource;
import com.partidoya.platform.courts.dto.UpdateCourtResource;
import com.partidoya.platform.courts.dto.CourtAvailabilityResourceFromEntityAssembler;
import com.partidoya.platform.courts.dto.CourtDetailsFromResourceAssembler;
import com.partidoya.platform.courts.dto.CourtReportResourceFromEntityAssembler;
import com.partidoya.platform.courts.dto.CourtResourceFromEntityAssembler;
import com.partidoya.platform.courts.dto.ManagedReservationResourceFromEntityAssembler;
import com.partidoya.platform.courts.services.CourtAvailabilityInput;
import com.partidoya.platform.courts.services.CourtAvailabilityService;
import com.partidoya.platform.courts.services.CourtManagementService;
import com.partidoya.platform.courts.services.CourtReportService;
import com.partidoya.platform.courts.services.CourtReservationService;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/courts", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Court Management", description = "Court owner administration endpoints")
public class CourtManagementController {
    private final CourtManagementService managementService;
    private final CourtAvailabilityService availabilityService;
    private final CourtReservationService reservationService;
    private final CourtReportService reportService;

    public CourtManagementController(
            CourtManagementService managementService,
            CourtAvailabilityService availabilityService,
            CourtReservationService reservationService,
            CourtReportService reportService) {
        this.managementService = managementService;
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;
        this.reportService = reportService;
    }

    @GetMapping("/managed/{ownerId}")
    public ResponseEntity<List<CourtResource>> getManagedCourts(@PathVariable Long ownerId) {
        var courts = managementService.listManagedCourts(new UserId(ownerId));
        return ResponseEntity.ok(courts.stream()
                .map(CourtResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/managed/{ownerId}/reservations")
    public ResponseEntity<List<ManagedReservationResource>> getManagedReservations(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var reservations = reservationService.listManaged(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to);
        return ResponseEntity.ok(reservations.stream()
                .map(ManagedReservationResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/managed/{ownerId}/availability")
    public ResponseEntity<List<CourtAvailabilityResource>> getManagedAvailability(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var availability = availabilityService.listManaged(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to);
        return ResponseEntity.ok(availability.stream()
                .map(CourtAvailabilityResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @PostMapping("/managed/{ownerId}/availability")
    public ResponseEntity<CourtAvailabilityResource> createAvailability(
            @PathVariable Long ownerId,
            @RequestBody SaveCourtAvailabilityResource resource) {
        var userId = new UserId(ownerId);
        var availability = availabilityService.create(userId, availabilityInput(resource));
        var view = availabilityService.findManagedView(userId, availability);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourtAvailabilityResourceFromEntityAssembler.toResourceFromEntity(view));
    }

    @PutMapping("/managed/{ownerId}/availability/{availabilityId}")
    public ResponseEntity<CourtAvailabilityResource> updateAvailability(
            @PathVariable Long ownerId,
            @PathVariable Long availabilityId,
            @RequestBody SaveCourtAvailabilityResource resource) {
        var userId = new UserId(ownerId);
        var availability = availabilityService.update(
                userId,
                new CourtAvailabilityId(availabilityId),
                availabilityInput(resource));
        var view = availabilityService.findManagedView(userId, availability);
        return ResponseEntity.ok(CourtAvailabilityResourceFromEntityAssembler.toResourceFromEntity(view));
    }

    @DeleteMapping("/managed/{ownerId}/availability/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long ownerId, @PathVariable Long availabilityId) {
        availabilityService.delete(new UserId(ownerId), new CourtAvailabilityId(availabilityId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/managed/{ownerId}/reports")
    public ResponseEntity<CourtReportResource> getManagedReport(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var report = reportService.generate(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to);
        return ResponseEntity.ok(CourtReportResourceFromEntityAssembler.toResourceFromEntity(report));
    }

    @PostMapping
    public ResponseEntity<CourtResource> createCourt(@RequestBody CreateCourtResource resource) {
        var court = managementService.create(
                new UserId(resource.ownerId()),
                CourtDetailsFromResourceAssembler.from(resource));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    @PutMapping("/{courtId}")
    public ResponseEntity<CourtResource> updateCourt(
            @PathVariable Long courtId,
            @RequestBody UpdateCourtResource resource) {
        var court = managementService.update(
                new CourtId(courtId),
                new UserId(resource.requesterId()),
                CourtDetailsFromResourceAssembler.from(resource));
        return ResponseEntity.ok(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    @PostMapping("/{courtId}/publication")
    public ResponseEntity<CourtResource> publishCourt(
            @PathVariable Long courtId,
            @RequestBody PublishCourtResource resource) {
        var court = managementService.setPublication(
                new CourtId(courtId),
                new UserId(resource.requesterId()),
                resource.published());
        return ResponseEntity.ok(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    private CourtAvailabilityInput availabilityInput(SaveCourtAvailabilityResource resource) {
        return new CourtAvailabilityInput(
                new CourtId(resource.courtId()),
                resource.date(),
                resource.allDay(),
                resource.startTime(),
                resource.endTime(),
                CourtAvailabilityType.valueOf(resource.type()),
                resource.reason());
    }
}
