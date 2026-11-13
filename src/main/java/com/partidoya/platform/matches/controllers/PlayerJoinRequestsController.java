package com.partidoya.platform.matches.controllers;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.services.UserService;
import com.partidoya.platform.matches.models.PlayerJoinRequest;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.storage.PaymentProofStorage;
import com.partidoya.platform.matches.dto.PlayerJoinRequestResource;
import com.partidoya.platform.matches.dto.RejectJoinRequestResource;
import com.partidoya.platform.matches.dto.PlayerJoinRequestResourceFromEntityAssembler;
import com.partidoya.platform.matches.services.JoinRequestService;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.auth.security.CurrentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping(value = "/api/v1/matches/{matchId}/join-requests")
@Tag(name = "Player Join Requests", description = "Yape payment proof workflow")
public class PlayerJoinRequestsController {
    private final JoinRequestService joinRequestService;
    private final PaymentProofStorage proofStorage;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public PlayerJoinRequestsController(JoinRequestService joinRequestService,
                                        PaymentProofStorage proofStorage,
                                        CurrentUserService currentUserService,
                                        UserService userService) {
        this.joinRequestService = joinRequestService;
        this.proofStorage = proofStorage;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    /** Resuelve los nombres de todos los jugadores con una sola consulta. */
    private List<PlayerJoinRequestResource> toResources(List<PlayerJoinRequest> requests, Long requesterId) {
        Map<Long, String> names = userService.findDisplayNames(
                requests.stream().map(PlayerJoinRequest::getPlayerId).distinct().toList());
        return requests.stream()
                .map(request -> PlayerJoinRequestResourceFromEntityAssembler.toResourceFromEntity(request, requesterId, names))
                .toList();
    }

    private PlayerJoinRequestResource toResource(PlayerJoinRequest request, Long requesterId) {
        return toResources(List.of(request), requesterId).getFirst();
    }

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PlayerJoinRequestResource> submitProof(@PathVariable Long matchId,
                                                                 @RequestParam Long userId,
                                                                 @RequestPart MultipartFile proof) throws IOException {
        var authenticatedUserId = currentUserService.requireSelf(userId);
        var stored = proofStorage.store(proof.getOriginalFilename(), proof.getContentType(), proof.getSize(),
                proof.getInputStream());
        var request = joinRequestService.submitProof(new MatchId(matchId), authenticatedUserId, stored);
        return ResponseEntity.ok(toResource(request, userId));
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PlayerJoinRequestResource>> listRequests(@PathVariable Long matchId,
                                                                        @RequestParam Long requesterId) {
        var authenticatedUserId = currentUserService.requireSelf(requesterId);
        var requests = joinRequestService.findAuthorized(new MatchId(matchId), authenticatedUserId);
        return ResponseEntity.ok(toResources(requests, requesterId));
    }

    @PostMapping(value = "/{requestId}/approve", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PlayerJoinRequestResource> approve(@PathVariable Long requestId,
                                                             @RequestParam Long reviewerId) {
        var request = joinRequestService.approve(requestId, currentUserService.requireSelf(reviewerId));
        return ResponseEntity.ok(toResource(request, reviewerId));
    }

    @PostMapping(value = "/{requestId}/reject", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PlayerJoinRequestResource> reject(@PathVariable Long requestId,
                                                            @RequestBody RejectJoinRequestResource resource) {
        var request = joinRequestService.reject(requestId, currentUserService.requireSelf(resource.reviewerId()), resource.rejectionReason());
        return ResponseEntity.ok(toResource(request, resource.reviewerId()));
    }

    @GetMapping("/{requestId}/proof")
    public ResponseEntity<Resource> getProof(@PathVariable Long requestId, @RequestParam Long requesterId) {
        var request = joinRequestService.findAuthorizedById(requestId, currentUserService.requireSelf(requesterId))
                .orElseThrow(() -> new ResourceNotFoundException("PlayerJoinRequest", requestId.toString()));
        var resource = new FileSystemResource(proofStorage.resolve(request.getProofStorageKey()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(request.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + request.getOriginalFileName() + "\"")
                .body(resource);
    }
}
