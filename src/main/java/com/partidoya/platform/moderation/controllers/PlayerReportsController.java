package com.partidoya.platform.moderation.controllers;

import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.moderation.dto.PlayerReportResourceAssembler;
import com.partidoya.platform.moderation.dto.SubmitPlayerReportResource;
import com.partidoya.platform.moderation.dto.SubmittedPlayerReportResource;
import com.partidoya.platform.moderation.models.SubmitPlayerReportCommand;
import com.partidoya.platform.moderation.services.PlayerReportService;
import com.partidoya.platform.users.models.UserId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** Reportes enviados por jugadores. Quien reporta siempre se toma del token, nunca del cuerpo. */
@RestController
@RequestMapping(value = "/api/v1/matches/{matchId}/reports", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Player Reports", description = "Report players from a match")
public class PlayerReportsController {
    private final PlayerReportService reportService;
    private final CurrentUserService currentUserService;

    public PlayerReportsController(PlayerReportService reportService, CurrentUserService currentUserService) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<SubmittedPlayerReportResource> submit(@PathVariable Long matchId,
                                                                @RequestBody SubmitPlayerReportResource resource) {
        if (resource.reportedPlayerId() == null) {
            throw new IllegalArgumentException("reportedPlayerId is required");
        }
        var command = new SubmitPlayerReportCommand(
                new MatchId(matchId),
                currentUserService.userId(),
                new UserId(resource.reportedPlayerId()),
                resource.reason(),
                resource.details());
        var report = reportService.submit(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PlayerReportResourceAssembler.toSubmittedResource(report));
    }

    /** Ids de los jugadores que el usuario autenticado ya reportó en este partido. */
    @GetMapping("/mine")
    public ResponseEntity<List<Long>> findMine(@PathVariable Long matchId) {
        var reported = reportService.findPlayersReportedBy(new MatchId(matchId), currentUserService.userId());
        return ResponseEntity.ok(reported.stream().map(UserId::value).toList());
    }
}
