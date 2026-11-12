package com.partidoya.platform.matches.controllers;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.services.UserService;
import com.partidoya.platform.matches.dto.CancelMatchResource;
import com.partidoya.platform.matches.dto.CreateMatchResource;
import com.partidoya.platform.matches.dto.JoinMatchResource;
import com.partidoya.platform.matches.dto.LeaveMatchResource;
import com.partidoya.platform.matches.dto.MatchResource;
import com.partidoya.platform.matches.dto.CreateMatchCommandFromResourceAssembler;
import com.partidoya.platform.matches.dto.MatchResourceFromEntityAssembler;
import com.partidoya.platform.matches.services.MatchService;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.auth.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/matches", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Matches", description = "Match management endpoints")
public class MatchesController {
    private final MatchService matchService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public MatchesController(MatchService matchService, CurrentUserService currentUserService, UserService userService) {
        this.matchService = matchService;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    private MatchResource toResource(Match match) {
        return toResources(List.of(match)).getFirst();
    }

    /** Resuelve los nombres de todos los jugadores de una sola consulta. */
    private List<MatchResource> toResources(List<Match> matches) {
        var userIds = matches.stream()
                .flatMap(match -> MatchResourceFromEntityAssembler.playerIds(match).stream())
                .collect(Collectors.toSet());
        var names = userService.findDisplayNames(userIds);
        return matches.stream()
                .map(match -> MatchResourceFromEntityAssembler.toResourceFromEntity(match, names))
                .toList();
    }

    @PostMapping
    public ResponseEntity<MatchResource> createMatch(@RequestBody CreateMatchResource resource) {
        var command = CreateMatchCommandFromResourceAssembler.toCommandFromResource(resource, currentUserService.userId());
        var match = matchService.create(command);
        var body = toResource(match);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<List<MatchResource>> getOpenMatches(
            @RequestParam(required = false) String sport) {
        var matches = matchService.searchOpen(sport);
        var body = toResources(matches);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResource> getMatchById(@PathVariable Long matchId) {
        var match = matchService.findById(new MatchId(matchId))
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.toString()));
        var body = toResource(match);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{matchId}/join")
    public ResponseEntity<MatchResource> joinMatch(
            @PathVariable Long matchId,
            @RequestBody JoinMatchResource resource) {
        var match = matchService.join(new MatchId(matchId), currentUserService.requireSelf(resource.userId()));
        var body = toResource(match);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/organized/{userId}")
    public ResponseEntity<List<MatchResource>> getMatchesByOrganizer(@PathVariable Long userId) {
        var matches = matchService.findByOrganizer(currentUserService.requireSelf(userId));
        var body = toResources(matches);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/joined/{userId}")
    public ResponseEntity<List<MatchResource>> getMatchesByParticipant(@PathVariable Long userId) {
        var matches = matchService.findByParticipant(currentUserService.requireSelf(userId));
        var body = toResources(matches);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{matchId}/leave")
    public ResponseEntity<MatchResource> leaveMatch(
            @PathVariable Long matchId,
            @RequestBody LeaveMatchResource resource) {
        var match = matchService.leave(new MatchId(matchId), currentUserService.requireSelf(resource.userId()));
        var body = toResource(match);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{matchId}/cancel")
    public ResponseEntity<MatchResource> cancelMatch(
            @PathVariable Long matchId,
            @RequestBody CancelMatchResource resource) {
        var match = matchService.cancel(new MatchId(matchId), currentUserService.requireSelf(resource.requesterId()));
        var body = toResource(match);
        return ResponseEntity.ok(body);
    }
}
