package com.partidoya.platform.matches.services;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.users.services.PlanPolicy;
import com.partidoya.platform.matches.models.PlayerJoinRequest;
import com.partidoya.platform.matches.models.JoinRequestStatus;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.matches.repositories.PlayerJoinRequestRepository;
import com.partidoya.platform.matches.storage.StoredPaymentProof;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JoinRequestService {
    private final PlayerJoinRequestRepository joinRequestRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JoinRequestService(
            PlayerJoinRequestRepository joinRequestRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.joinRequestRepository = joinRequestRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public PlayerJoinRequest submitProof(MatchId matchId, UserId playerId, StoredPaymentProof proof) {
        requirePlayer(playerId);
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.value().toString()));
        if (!match.acceptsPaymentProofs()) {
            throw new ForbiddenActionException("This match does not accept payment proofs");
        }
        if (match.getOrganizerId().value().equals(playerId.value())) {
            throw new ForbiddenActionException("Organizer cannot submit a proof for their own match");
        }
        if (match.getParticipants().stream().anyMatch(id -> id.value().equals(playerId.value()))) {
            throw new ResourceConflictException("PlayerJoinRequest", "player already joined");
        }
        if (joinRequestRepository.existsByMatchIdAndPlayerIdAndStatusIn(
                matchId,
                playerId,
                List.of(JoinRequestStatus.PENDING_PAYMENT_VERIFICATION, JoinRequestStatus.CONFIRMED))) {
            throw new ResourceConflictException("PlayerJoinRequest", "player already has an active proof");
        }
        var latest = joinRequestRepository.findLatestByMatchIdAndPlayerId(matchId, playerId);
        if (latest.isPresent() && latest.get().getStatus() == JoinRequestStatus.REJECTED) {
            var request = latest.get();
            request.replaceProof(
                    proof.storageKey(),
                    proof.originalFileName(),
                    proof.contentType(),
                    match.getPlayerPaymentAmount().value());
            return joinRequestRepository.save(request);
        }
        return joinRequestRepository.save(new PlayerJoinRequest(
                matchId,
                playerId,
                proof.storageKey(),
                proof.originalFileName(),
                proof.contentType(),
                match.getPlayerPaymentAmount().value()));
    }

    public PlayerJoinRequest approve(Long requestId, UserId reviewerId) {
        var request = requireRequest(requestId);
        var match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match",
                        request.getMatchId().value().toString()));
        requireOrganizer(match.getOrganizerId(), reviewerId);
        match.confirmParticipant(request.getPlayerId());
        request.approve(reviewerId);
        var savedMatch = matchRepository.save(match);
        var savedRequest = joinRequestRepository.save(request);
        eventPublisher.publishEvent(new MatchEvent.PaymentApproved(
                savedMatch.getId(), reviewerId, savedRequest.getPlayerId()));
        if (savedMatch.getStatus() == MatchStatus.FULL) {
            eventPublisher.publishEvent(new MatchEvent.MatchFilled(savedMatch.getId(), reviewerId));
        }
        return savedRequest;
    }

    public PlayerJoinRequest reject(Long requestId, UserId reviewerId, String reason) {
        var request = requireRequest(requestId);
        var match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match",
                        request.getMatchId().value().toString()));
        requireOrganizer(match.getOrganizerId(), reviewerId);
        request.reject(reviewerId, reason);
        var savedRequest = joinRequestRepository.save(request);
        eventPublisher.publishEvent(new MatchEvent.PaymentRejected(
                request.getMatchId(), reviewerId, savedRequest.getPlayerId(), savedRequest.getRejectionReason()));
        return savedRequest;
    }

    @Transactional(readOnly = true)
    public List<PlayerJoinRequest> findAuthorized(MatchId matchId, UserId requesterId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.value().toString()));
        var requests = joinRequestRepository.findByMatchId(matchId);
        if (match.getOrganizerId().value().equals(requesterId.value())) return requests;
        return requests.stream()
                .filter(request -> request.getPlayerId().value().equals(requesterId.value()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PlayerJoinRequest> findAuthorizedById(Long requestId, UserId requesterId) {
        var request = joinRequestRepository.findById(requestId);
        if (request.isEmpty()) return Optional.empty();
        var match = matchRepository.findById(request.get().getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match",
                        request.get().getMatchId().value().toString()));
        var isOrganizer = match.getOrganizerId().value().equals(requesterId.value());
        var isOwner = request.get().getPlayerId().value().equals(requesterId.value());
        if (!isOrganizer && !isOwner) {
            throw new ForbiddenActionException("You cannot access this payment proof");
        }
        return request;
    }

    private PlayerJoinRequest requireRequest(Long requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PlayerJoinRequest", requestId.toString()));
    }

    private void requirePlayer(UserId playerId) {
        var user = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", playerId.value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(user)) {
            throw new ForbiddenActionException("Only player accounts can submit payment proofs");
        }
    }

    private void requireOrganizer(UserId organizerId, UserId reviewerId) {
        if (!organizerId.value().equals(reviewerId.value())) {
            throw new ForbiddenActionException("Only the organizer can review payment proofs");
        }
    }
}
