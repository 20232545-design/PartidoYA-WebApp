package com.partidoya.platform.moderation.services;

import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.Match;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import com.partidoya.platform.matches.repositories.MatchRepository;
import com.partidoya.platform.moderation.models.PlayerReport;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.moderation.models.ReportReason;
import com.partidoya.platform.moderation.models.SubmitPlayerReportCommand;
import com.partidoya.platform.moderation.repositories.PlayerReportRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerReportServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-12T18:00:00Z");
    private static final MatchId MATCH_ID = new MatchId(1L);
    private static final UserId ORGANIZER = new UserId(1L);
    private static final UserId PLAYER_A = new UserId(7L);
    private static final UserId PLAYER_B = new UserId(18L);
    private static final UserId OUTSIDER = new UserId(99L);
    private static final UserId ADMIN = new UserId(500L);

    private InMemoryPlayerReportRepository reportRepository;
    private PlayerReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = new InMemoryPlayerReportRepository();
        var matchRepository = new InMemoryMatchRepository(match(MATCH_ID, ORGANIZER, List.of(PLAYER_A, PLAYER_B)));
        service = new PlayerReportService(reportRepository, matchRepository, new NoUsersRepository(),
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void participantCanReportAnotherParticipant() {
        var report = service.submit(command(PLAYER_A, PLAYER_B, ReportReason.HARASSMENT));

        assertThat(report.getId()).isNotNull();
        assertThat(report.getStatus()).isEqualTo(PlayerReportStatus.PENDING);
        assertThat(report.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 11, 12, 18, 0));
    }

    @Test
    void participantCanReportTheOrganizer() {
        var report = service.submit(command(PLAYER_A, ORGANIZER, ReportReason.NO_SHOW));

        assertThat(report.getReportedPlayerId()).isEqualTo(ORGANIZER);
    }

    @Test
    void rejectsReporterWhoIsNotPartOfTheMatch() {
        assertThatThrownBy(() -> service.submit(command(OUTSIDER, PLAYER_B, ReportReason.HARASSMENT)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void rejectsReportedPlayerWhoIsNotPartOfTheMatch() {
        assertThatThrownBy(() -> service.submit(command(PLAYER_A, OUTSIDER, ReportReason.HARASSMENT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSelfReport() {
        assertThatThrownBy(() -> service.submit(command(PLAYER_A, PLAYER_A, ReportReason.HARASSMENT)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateReportForSameMatchAndPlayer() {
        service.submit(command(PLAYER_A, PLAYER_B, ReportReason.HARASSMENT));

        assertThatThrownBy(() -> service.submit(command(PLAYER_A, PLAYER_B, ReportReason.NO_SHOW)))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsReportForUnknownMatch() {
        var command = new SubmitPlayerReportCommand(new MatchId(404L), PLAYER_A, PLAYER_B, ReportReason.HARASSMENT, null);

        assertThatThrownBy(() -> service.submit(command)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsPlayersAlreadyReportedByUserInMatch() {
        service.submit(command(PLAYER_A, PLAYER_B, ReportReason.HARASSMENT));

        assertThat(service.findPlayersReportedBy(MATCH_ID, PLAYER_A)).containsExactly(PLAYER_B);
        assertThat(service.findPlayersReportedBy(MATCH_ID, PLAYER_B)).isEmpty();
    }

    @Test
    void searchFiltersByStatusAndFallsBackToGenericNames() {
        var pending = service.submit(command(PLAYER_A, PLAYER_B, ReportReason.HARASSMENT));
        var dismissed = service.submit(command(PLAYER_B, PLAYER_A, ReportReason.NO_SHOW));
        service.resolve(dismissed.getId(), ADMIN, PlayerReportStatus.DISMISSED, null);

        var page = service.search(PlayerReportStatus.PENDING, 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).singleElement().satisfies(view -> {
            assertThat(view.report().getId()).isEqualTo(pending.getId());
            assertThat(view.reportedPlayerName()).isEqualTo("Jugador #18");
            assertThat(view.matchTitle()).isEqualTo("Pichanga");
        });
    }

    @Test
    void searchClampsPageSize() {
        var page = service.search(null, -3, 10_000);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(PlayerReportService.MAX_PAGE_SIZE);
    }

    @Test
    void adminResolvesPendingReport() {
        var report = service.submit(command(PLAYER_A, PLAYER_B, ReportReason.HARASSMENT));

        var view = service.resolve(report.getId(), ADMIN, PlayerReportStatus.ACTION_TAKEN, "Advertencia enviada");

        assertThat(view.report().getStatus()).isEqualTo(PlayerReportStatus.ACTION_TAKEN);
        assertThat(view.report().getReviewedBy()).isEqualTo(ADMIN);
    }

    @Test
    void resolvingUnknownReportFails() {
        assertThatThrownBy(() -> service.resolve(404L, ADMIN, PlayerReportStatus.DISMISSED, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static SubmitPlayerReportCommand command(UserId reporter, UserId reported, ReportReason reason) {
        return new SubmitPlayerReportCommand(MATCH_ID, reporter, reported, reason, null);
    }

    private static Match match(MatchId id, UserId organizer, List<UserId> participants) {
        return new Match(id, organizer, "Futbol", new MatchTitle("Pichanga"), new MatchDescription("Test"),
                new Location("Lima"), new MatchDate(LocalDateTime.of(2026, 11, 20, 18, 0)), new TotalSlots(10),
                new MatchPrice(BigDecimal.ZERO), null, null, MatchStatus.OPEN, participants);
    }

    /** Fake en memoria que respeta el contrato de {@link PlayerReportRepository}. */
    private static final class InMemoryPlayerReportRepository implements PlayerReportRepository {
        private final Map<Long, PlayerReport> reports = new HashMap<>();
        private long nextId = 1;

        long count() {
            return reports.size();
        }

        @Override
        public Optional<PlayerReport> findById(Long id) {
            return Optional.ofNullable(reports.get(id));
        }

        @Override
        public boolean existsByMatchIdAndReporterIdAndReportedPlayerId(MatchId matchId, UserId reporterId,
                                                                       UserId reportedPlayerId) {
            return reports.values().stream().anyMatch(report ->
                    report.getMatchId().equals(matchId)
                            && report.getReporterId().equals(reporterId)
                            && report.getReportedPlayerId().equals(reportedPlayerId));
        }

        @Override
        public List<UserId> findReportedPlayerIdsByMatchIdAndReporterId(MatchId matchId, UserId reporterId) {
            return reports.values().stream()
                    .filter(report -> report.getMatchId().equals(matchId) && report.getReporterId().equals(reporterId))
                    .map(PlayerReport::getReportedPlayerId)
                    .toList();
        }

        @Override
        public List<PlayerReport> findByStatus(PlayerReportStatus status, int page, int size) {
            return reports.values().stream()
                    .filter(report -> status == null || report.getStatus() == status)
                    .sorted(Comparator.comparing(PlayerReport::getSubmittedAt).reversed())
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
        }

        @Override
        public long countByStatus(PlayerReportStatus status) {
            return reports.values().stream().filter(report -> status == null || report.getStatus() == status).count();
        }

        @Override
        public PlayerReport save(PlayerReport report) {
            var stored = report.getId() != null ? report : PlayerReport.restore(nextId++, report.getMatchId(),
                    report.getReporterId(), report.getReportedPlayerId(), report.getReason(), report.getDetails(),
                    report.getStatus(), report.getSubmittedAt(), report.getReviewedAt(), report.getReviewedBy(),
                    report.getResolutionNote());
            reports.put(stored.getId(), stored);
            return stored;
        }
    }

    private static final class InMemoryMatchRepository implements MatchRepository {
        private final Map<Long, Match> matches = new HashMap<>();

        InMemoryMatchRepository(Match... initial) {
            for (var match : initial) matches.put(match.getId().value(), match);
        }

        @Override
        public Optional<Match> findById(MatchId id) {
            return Optional.ofNullable(matches.get(id.value()));
        }

        @Override
        public Match save(Match match) {
            matches.put(match.getId().value(), match);
            return match;
        }

        @Override
        public List<Match> findAllByStatus(MatchStatus status) {
            return matches.values().stream().filter(match -> match.getStatus() == status).toList();
        }

        @Override
        public List<Match> findAllByStatusAndSport(MatchStatus status, String sport) {
            return findAllByStatus(status).stream().filter(match -> match.getSport().equals(sport)).toList();
        }

        @Override
        public List<Match> findAllByOrganizerId(UserId organizerId) {
            return matches.values().stream().filter(match -> match.getOrganizerId().equals(organizerId)).toList();
        }

        @Override
        public List<Match> findAllByParticipantId(UserId participantId) {
            return matches.values().stream().filter(match -> match.getParticipants().contains(participantId)).toList();
        }

        @Override
        public long countRelatedToUser(UserId userId) {
            return matches.values().stream().filter(match -> match.involves(userId)).count();
        }
    }

    /** Repositorio de usuarios vacío: obliga al servicio a usar los nombres de respaldo. */
    private static final class NoUsersRepository implements UserRepository {
        @Override
        public Optional<User> findById(UserId id) {
            return Optional.empty();
        }

        @Override
        public List<User> findAllByIds(Collection<UserId> ids) {
            return new ArrayList<>();
        }

        @Override
        public Optional<User> findByEmail(EmailAddress email) {
            return Optional.empty();
        }

        @Override
        public List<User> search(String search, Role role, int page, int size) {
            return List.of();
        }

        @Override
        public long countSearch(String search, Role role) {
            return 0;
        }

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public boolean existsByEmail(EmailAddress email) {
            return false;
        }
    }
}
