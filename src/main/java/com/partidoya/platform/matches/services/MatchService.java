package com.partidoya.platform.matches.services;

import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.users.services.PlanPolicy;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.CreateMatchCommand;
import com.partidoya.platform.matches.models.MatchEvent;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatchService {
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(
            MatchRepository matchRepository,
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            ApplicationEventPublisher eventPublisher) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }

    public Match create(CreateMatchCommand request) {
        requirePlayer(request.organizerId(), "create matches");
        return matchRepository.save(new Match(withReservationPrice(request)));
    }

    public Match join(MatchId matchId, UserId userId) {
        var match = requireMatch(matchId);
        requirePlayer(userId, "join matches");
        match.join(userId);
        var saved = matchRepository.save(match);
        eventPublisher.publishEvent(new MatchEvent.PlayerJoined(matchId, userId));
        if (saved.getStatus() == MatchStatus.FULL) {
            eventPublisher.publishEvent(new MatchEvent.MatchFilled(matchId, userId));
        }
        return saved;
    }

    public Match leave(MatchId matchId, UserId userId) {
        var match = requireMatch(matchId);
        requirePlayer(userId, "leave matches");
        match.leave(userId);
        var saved = matchRepository.save(match);
        eventPublisher.publishEvent(new MatchEvent.PlayerLeft(matchId, userId));
        return saved;
    }

    public Match cancel(MatchId matchId, UserId requesterId) {
        var match = requireMatch(matchId);
        match.cancel(requesterId);
        var saved = matchRepository.save(match);
        eventPublisher.publishEvent(new MatchEvent.MatchCancelled(matchId, requesterId));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Match> findById(MatchId matchId) {
        return matchRepository.findById(matchId);
    }

    @Transactional(readOnly = true)
    public List<Match> searchOpen(String sport) {
        if (sport != null && !sport.isBlank()) {
            return matchRepository.findAllByStatusAndSport(MatchStatus.OPEN, sport);
        }
        return matchRepository.findAllByStatus(MatchStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<Match> findByOrganizer(UserId organizerId) {
        return matchRepository.findAllByOrganizerId(organizerId);
    }

    @Transactional(readOnly = true)
    public List<Match> findByParticipant(UserId participantId) {
        return matchRepository.findAllByParticipantId(participantId);
    }

    private Match requireMatch(MatchId matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.value().toString()));
    }

    private void requirePlayer(UserId userId, String action) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(user)) {
            throw new ForbiddenActionException("Only player accounts can " + action);
        }
    }

    private CreateMatchCommand withReservationPrice(CreateMatchCommand request) {
        if (request.courtReservationId() == null) return request;
        var reservation = reservationRepository.findById(new ReservationId(request.courtReservationId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation",
                        request.courtReservationId().toString()));
        if (!reservation.isConfirmedFor(request.organizerId())) {
            throw new ForbiddenActionException("Only confirmed paid reservations can create matches");
        }
        return new CreateMatchCommand(
                request.organizerId(),
                request.courtReservationId(),
                request.sport(),
                request.title(),
                request.description(),
                request.address(),
                request.matchDate(),
                request.totalSlots(),
                new MatchPrice(reservation.getPrice()),
                request.latitude(),
                request.longitude(),
                request.requiresPlayerPayment(),
                request.yapePhone());
    }
}
