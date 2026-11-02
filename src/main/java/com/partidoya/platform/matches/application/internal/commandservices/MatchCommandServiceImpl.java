package com.partidoya.platform.matches.application.internal.commandservices;

import com.partidoya.platform.iam.domain.repositories.UserRepository;
import com.partidoya.platform.iam.domain.services.PlanPolicy;
import com.partidoya.platform.courts.models.ReservationId;
import com.partidoya.platform.courts.repositories.ReservationRepository;
import com.partidoya.platform.matches.application.commandservices.MatchCommandService;
import com.partidoya.platform.matches.domain.model.aggregates.Match;
import com.partidoya.platform.matches.domain.model.commands.CancelMatchCommand;
import com.partidoya.platform.matches.domain.model.commands.CreateMatchCommand;
import com.partidoya.platform.matches.domain.model.commands.JoinMatchCommand;
import com.partidoya.platform.matches.domain.model.commands.LeaveMatchCommand;
import com.partidoya.platform.matches.domain.model.valueobjects.MatchPrice;
import com.partidoya.platform.matches.domain.repositories.MatchRepository;
import com.partidoya.platform.shared.domain.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MatchCommandServiceImpl implements MatchCommandService {
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public MatchCommandServiceImpl(MatchRepository matchRepository, UserRepository userRepository,
                                   ReservationRepository reservationRepository) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Match handle(CreateMatchCommand command) {
        var organizer = userRepository.findById(command.organizerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", command.organizerId().value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(organizer)) {
            throw new ForbiddenActionException("Only player accounts can create matches");
        }
        var effectiveCommand = command;
        if (command.courtReservationId() != null) {
            var reservation = reservationRepository.findById(new ReservationId(command.courtReservationId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", command.courtReservationId().toString()));
            if (!reservation.isConfirmedFor(command.organizerId())) {
                throw new ForbiddenActionException("Only confirmed paid reservations can create matches");
            }
            effectiveCommand = new CreateMatchCommand(command.organizerId(), command.courtReservationId(),
                    command.sport(), command.title(), command.description(), command.address(), command.matchDate(),
                    command.totalSlots(), new MatchPrice(reservation.getPrice()), command.latitude(), command.longitude(),
                    command.requiresPlayerPayment(), command.yapePhone());
        }
        var match = new Match(effectiveCommand);
        return matchRepository.save(match);
    }

    @Override
    public Match handle(JoinMatchCommand command) {
        var match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match", command.matchId().value().toString()));
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", command.userId().value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(user)) {
            throw new ForbiddenActionException("Only player accounts can join matches");
        }
        match.join(command.userId());
        return matchRepository.save(match);
    }

    @Override
    public Match handle(CancelMatchCommand command) {
        var match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match", command.matchId().value().toString()));
        match.cancel(command.requesterId());
        return matchRepository.save(match);
    }

    @Override
    public Match handle(LeaveMatchCommand command) {
        var match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match", command.matchId().value().toString()));
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", command.userId().value().toString()));
        if (!PlanPolicy.canUsePlayerFeatures(user)) {
            throw new ForbiddenActionException("Only player accounts can leave matches");
        }
        match.leave(command.userId());
        return matchRepository.save(match);
    }
}
