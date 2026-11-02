package com.partidoya.platform.courts.interfaces.rest;

import com.partidoya.platform.courts.application.commandservices.CourtCommandService;
import com.partidoya.platform.courts.application.queryservices.CourtQueryService;
import com.partidoya.platform.courts.domain.model.commands.CreateCourtAvailabilityCommand;
import com.partidoya.platform.courts.domain.model.commands.DeleteCourtAvailabilityCommand;
import com.partidoya.platform.courts.domain.model.commands.PublishCourtCommand;
import com.partidoya.platform.courts.domain.model.commands.UpdateCourtAvailabilityCommand;
import com.partidoya.platform.courts.domain.model.queries.GetManagedAvailabilityQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedCourtReportQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedCourtsQuery;
import com.partidoya.platform.courts.domain.model.queries.GetManagedReservationsQuery;
import com.partidoya.platform.courts.domain.model.valueobjects.CourtAvailabilityId;
import com.partidoya.platform.courts.domain.model.valueobjects.CourtAvailabilityType;
import com.partidoya.platform.courts.domain.model.valueobjects.CourtId;
import com.partidoya.platform.courts.interfaces.rest.resources.CourtAvailabilityResource;
import com.partidoya.platform.courts.interfaces.rest.resources.CourtReportResource;
import com.partidoya.platform.courts.interfaces.rest.resources.CourtResource;
import com.partidoya.platform.courts.interfaces.rest.resources.CreateCourtResource;
import com.partidoya.platform.courts.interfaces.rest.resources.ManagedReservationResource;
import com.partidoya.platform.courts.interfaces.rest.resources.PublishCourtResource;
import com.partidoya.platform.courts.interfaces.rest.resources.SaveCourtAvailabilityResource;
import com.partidoya.platform.courts.interfaces.rest.resources.UpdateCourtResource;
import com.partidoya.platform.courts.interfaces.rest.transform.CourtAvailabilityResourceFromEntityAssembler;
import com.partidoya.platform.courts.interfaces.rest.transform.CourtCommandFromResourceAssembler;
import com.partidoya.platform.courts.interfaces.rest.transform.CourtReportResourceFromEntityAssembler;
import com.partidoya.platform.courts.interfaces.rest.transform.CourtResourceFromEntityAssembler;
import com.partidoya.platform.courts.interfaces.rest.transform.ManagedReservationResourceFromEntityAssembler;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
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
    private final CourtCommandService courtCommandService;
    private final CourtQueryService courtQueryService;

    public CourtManagementController(CourtCommandService courtCommandService, CourtQueryService courtQueryService) {
        this.courtCommandService = courtCommandService;
        this.courtQueryService = courtQueryService;
    }

    @GetMapping("/managed/{ownerId}")
    public ResponseEntity<List<CourtResource>> getManagedCourts(@PathVariable Long ownerId) {
        var courts = courtQueryService.handle(new GetManagedCourtsQuery(new UserId(ownerId)));
        var body = courts.stream().map(CourtResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/managed/{ownerId}/reservations")
    public ResponseEntity<List<ManagedReservationResource>> getManagedReservations(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var reservations = courtQueryService.handle(new GetManagedReservationsQuery(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to));
        var body = reservations.stream()
                .map(ManagedReservationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/managed/{ownerId}/availability")
    public ResponseEntity<List<CourtAvailabilityResource>> getManagedAvailability(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var availability = courtQueryService.handle(new GetManagedAvailabilityQuery(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to));
        var body = availability.stream()
                .map(CourtAvailabilityResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/managed/{ownerId}/availability")
    public ResponseEntity<CourtAvailabilityResource> createAvailability(
            @PathVariable Long ownerId,
            @RequestBody SaveCourtAvailabilityResource resource) {
        var availability = courtCommandService.handle(new CreateCourtAvailabilityCommand(
                new UserId(ownerId),
                new CourtId(resource.courtId()),
                resource.date(),
                resource.allDay(),
                resource.startTime(),
                resource.endTime(),
                CourtAvailabilityType.valueOf(resource.type()),
                resource.reason()));
        return ResponseEntity.status(HttpStatus.CREATED).body(findAvailability(ownerId, availability.getId().value(),
                availability.getCourtId().value(), availability.getDate()));
    }

    @PutMapping("/managed/{ownerId}/availability/{availabilityId}")
    public ResponseEntity<CourtAvailabilityResource> updateAvailability(
            @PathVariable Long ownerId,
            @PathVariable Long availabilityId,
            @RequestBody SaveCourtAvailabilityResource resource) {
        var availability = courtCommandService.handle(new UpdateCourtAvailabilityCommand(
                new UserId(ownerId),
                new CourtAvailabilityId(availabilityId),
                new CourtId(resource.courtId()),
                resource.date(),
                resource.allDay(),
                resource.startTime(),
                resource.endTime(),
                CourtAvailabilityType.valueOf(resource.type()),
                resource.reason()));
        return ResponseEntity.ok(findAvailability(ownerId, availability.getId().value(),
                availability.getCourtId().value(), availability.getDate()));
    }

    @DeleteMapping("/managed/{ownerId}/availability/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long ownerId, @PathVariable Long availabilityId) {
        courtCommandService.handle(new DeleteCourtAvailabilityCommand(
                new UserId(ownerId),
                new CourtAvailabilityId(availabilityId)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/managed/{ownerId}/reports")
    public ResponseEntity<CourtReportResource> getManagedReport(
            @PathVariable Long ownerId,
            @RequestParam(required = false) Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var report = courtQueryService.handle(new GetManagedCourtReportQuery(
                new UserId(ownerId),
                courtId == null ? null : new CourtId(courtId),
                from,
                to));
        return ResponseEntity.ok(CourtReportResourceFromEntityAssembler.toResourceFromEntity(report));
    }

    @PostMapping
    public ResponseEntity<CourtResource> createCourt(@RequestBody CreateCourtResource resource) {
        var court = courtCommandService.handle(CourtCommandFromResourceAssembler.toCommandFromResource(resource));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    @PutMapping("/{courtId}")
    public ResponseEntity<CourtResource> updateCourt(
            @PathVariable Long courtId,
            @RequestBody UpdateCourtResource resource) {
        var court = courtCommandService.handle(
                CourtCommandFromResourceAssembler.toCommandFromResource(courtId, resource));
        return ResponseEntity.ok(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    @PostMapping("/{courtId}/publication")
    public ResponseEntity<CourtResource> publishCourt(
            @PathVariable Long courtId,
            @RequestBody PublishCourtResource resource) {
        var court = courtCommandService.handle(new PublishCourtCommand(
                new CourtId(courtId),
                new UserId(resource.requesterId()),
                resource.published()));
        return ResponseEntity.ok(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    private CourtAvailabilityResource findAvailability(Long ownerId, Long availabilityId, Long courtId, LocalDate date) {
        var view = courtQueryService.handle(new GetManagedAvailabilityQuery(
                        new UserId(ownerId),
                        new CourtId(courtId),
                        date,
                        date))
                .stream()
                .filter(item -> item.id().equals(availabilityId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CourtAvailability", availabilityId.toString()));
        return CourtAvailabilityResourceFromEntityAssembler.toResourceFromEntity(view);
    }
}
