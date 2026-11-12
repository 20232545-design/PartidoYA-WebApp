package com.partidoya.platform.moderation.services;

import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportPage;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.moderation.models.PlayerReportView;
import com.partidoya.platform.moderation.models.SubmitPlayerReportCommand;
import com.partidoya.platform.moderation.repositories.PlayerReportRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class PlayerReportService {
    static final int MAX_PAGE_SIZE = 50;

    private final PlayerReportRepository reportRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PlayerReportService(PlayerReportRepository reportRepository, MatchRepository matchRepository,
                               UserRepository userRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public PlayerReport submit(SubmitPlayerReportCommand command) {
        var match = requireMatch(command.matchId());
        if (!match.involves(command.reporterId())) {
            throw new ForbiddenActionException("Only players of this match can report other players");
        }
        if (!match.involves(command.reportedPlayerId())) {
            throw new IllegalArgumentException("The reported player did not take part in this match");
        }
        var report = PlayerReport.submit(command.matchId(), command.reporterId(), command.reportedPlayerId(),
                command.reason(), command.details(), now());
        if (reportRepository.existsByMatchIdAndReporterIdAndReportedPlayerId(
                command.matchId(), command.reporterId(), command.reportedPlayerId())) {
            throw new ResourceConflictException("PlayerReport", "you already reported this player for this match");
        }
        return reportRepository.save(report);
    }

    /** Jugadores que el usuario ya reportó en el partido, para que la UI no ofrezca reportarlos de nuevo. */
    @Transactional(readOnly = true)
    public List<UserId> findPlayersReportedBy(MatchId matchId, UserId reporterId) {
        return reportRepository.findReportedPlayerIdsByMatchIdAndReporterId(matchId, reporterId);
    }

    @Transactional(readOnly = true)
    public PlayerReportPage search(PlayerReportStatus status, int page, int size) {
        var safePage = Math.max(page, 0);
        var safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        var reports = reportRepository.findByStatus(status, safePage, safeSize);
        return new PlayerReportPage(toViews(reports), reportRepository.countByStatus(status), safePage, safeSize);
    }

    public PlayerReportView resolve(Long reportId, UserId adminId, PlayerReportStatus outcome, String note) {
        var report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("PlayerReport", reportId.toString()));
        report.resolve(adminId, outcome, note, now());
        return toViews(List.of(reportRepository.save(report))).getFirst();
    }

    private List<PlayerReportView> toViews(List<PlayerReport> reports) {
        var userIds = reports.stream()
                .flatMap(report -> Stream.of(report.getReporterId(), report.getReportedPlayerId()))
                .toList();
        Map<Long, String> names = userRepository.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(user -> user.getId().value(), user -> user.getFullName().value(),
                        (first, second) -> first));
        var matchTitles = new HashMap<Long, String>();
        return reports.stream()
                .map(report -> new PlayerReportView(
                        report,
                        displayName(names, report.getReporterId()),
                        displayName(names, report.getReportedPlayerId()),
                        matchTitles.computeIfAbsent(report.getMatchId().value(), id -> matchTitle(report.getMatchId()))))
                .toList();
    }

    private String matchTitle(MatchId matchId) {
        return matchRepository.findById(matchId)
                .map(match -> match.getTitle().value())
                .orElse("Partido #" + matchId.value());
    }

    private static String displayName(Map<Long, String> names, UserId userId) {
        return names.getOrDefault(userId.value(), "Jugador #" + userId.value());
    }

    private Match requireMatch(MatchId matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.value().toString()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
