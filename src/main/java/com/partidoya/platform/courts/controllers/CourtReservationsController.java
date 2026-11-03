package com.partidoya.platform.courts.controllers;

import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.dto.ReservationResource;
import com.partidoya.platform.courts.dto.ReserveCourtResource;
import com.partidoya.platform.courts.dto.ReservationResourceFromEntityAssembler;
import com.partidoya.platform.courts.services.CourtReservationRequest;
import com.partidoya.platform.courts.services.CourtReservationService;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.auth.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/courts", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Court Reservations", description = "Court reservation endpoints")
public class CourtReservationsController {
    private final CourtReservationService reservationService;
    private final CurrentUserService currentUserService;

    public CourtReservationsController(CourtReservationService reservationService, CurrentUserService currentUserService) {
        this.reservationService = reservationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{courtId}/reservations")
    public ResponseEntity<ReservationResource> reserveCourt(
            @PathVariable Long courtId,
            @RequestBody ReserveCourtResource resource) {
        var reservation = reservationService.reserve(new CourtReservationRequest(
                new CourtId(courtId),
                currentUserService.requireSelf(resource.userId()),
                resource.date(),
                resource.startTime(),
                resource.endTime(),
                resource.paymentMethod(),
                resource.culqiToken(),
                resource.payerEmail(),
                resource.idempotencyKey()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservationResourceFromEntityAssembler.toResourceFromEntity(reservation));
    }
}
