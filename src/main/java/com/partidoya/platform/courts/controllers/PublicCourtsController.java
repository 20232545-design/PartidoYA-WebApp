package com.partidoya.platform.courts.controllers;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.dto.CourtResource;
import com.partidoya.platform.courts.dto.CourtResourceFromEntityAssembler;
import com.partidoya.platform.courts.services.CourtAvailabilityService;
import com.partidoya.platform.courts.services.CourtSearchService;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/courts", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Courts", description = "Published court discovery endpoints")
public class PublicCourtsController {
    private final CourtSearchService courtSearchService;
    private final CourtAvailabilityService availabilityService;

    public PublicCourtsController(
            CourtSearchService courtSearchService,
            CourtAvailabilityService availabilityService) {
        this.courtSearchService = courtSearchService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<CourtResource>> searchPublishedCourts(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String district) {
        var courts = courtSearchService.searchPublished(sport, district);
        var body = courts.stream().map(CourtResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{courtId}")
    public ResponseEntity<CourtResource> getCourtById(@PathVariable Long courtId) {
        var court = courtSearchService.findPublishedById(new CourtId(courtId))
                .orElseThrow(() -> new ResourceNotFoundException("Court", courtId.toString()));
        return ResponseEntity.ok(CourtResourceFromEntityAssembler.toResourceFromEntity(court));
    }

    @GetMapping("/{courtId}/available-schedules")
    public ResponseEntity<List<String>> getAvailableSchedules(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.findReservableSchedules(new CourtId(courtId), date));
    }
}
