package com.partidoya.platform.teams.services;

import com.partidoya.platform.auth.security.AuthenticatedUser;
import com.partidoya.platform.profiles.repositories.ProfilePersistenceRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.teams.dto.TeamResources;
import com.partidoya.platform.teams.models.TeamChallengeEntity;
import com.partidoya.platform.teams.models.TeamEntity;
import com.partidoya.platform.teams.models.TeamMembershipEntity;
import com.partidoya.platform.teams.models.TeamRequestEntity;
import com.partidoya.platform.teams.repositories.TeamChallengeRepository;
import com.partidoya.platform.teams.repositories.TeamMembershipRepository;
import com.partidoya.platform.teams.repositories.TeamRepository;
import com.partidoya.platform.teams.repositories.TeamRequestRepository;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.repositories.UserPersistenceEntity;
import com.partidoya.platform.users.repositories.UserPersistenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class TeamServiceTest {
    private TeamRepository teams;
    private TeamMembershipRepository memberships;
    private TeamRequestRepository requests;
    private TeamChallengeRepository challenges;
    private UserPersistenceRepository users;
    private TeamService service;

    @BeforeEach
    void setUp() {
        teams = mock(TeamRepository.class);
        memberships = mock(TeamMembershipRepository.class);
        requests = mock(TeamRequestRepository.class);
        challenges = mock(TeamChallengeRepository.class);
        users = mock(UserPersistenceRepository.class);
        var profiles = mock(ProfilePersistenceRepository.class);
        var images = mock(TeamImageStorage.class);
        service = new TeamService(teams, memberships, requests, challenges, users, profiles, images);
        when(teams.save(any())).thenAnswer(invocation -> {
            var entity = invocation.<TeamEntity>getArgument(0);
            if (entity.getId() == null) entity.setId(10L);
            return entity;
        });
        when(memberships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.save(any())).thenAnswer(invocation -> {
            var entity = invocation.<TeamRequestEntity>getArgument(0);
            if (entity.getId() == null) entity.setId(20L);
            return entity;
        });
        when(challenges.save(any())).thenAnswer(invocation -> {
            var entity = invocation.<TeamChallengeEntity>getArgument(0);
            if (entity.getId() == null) entity.setId(30L);
            return entity;
        });
    }

    @Test
    void createsValidTeamAndMakesCreatorItsOnlyCaptain() {
        when(teams.existsByNormalizedName("los cracks")).thenReturn(false);
        when(users.findById(1L)).thenReturn(Optional.of(player(1L, "Ana")));
        when(memberships.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership(10L, 1L, "CAPTAIN")));
        when(memberships.countByTeamIdAndStatus(10L, "ACTIVE")).thenReturn(1L);

        var result = service.create(auth(1L), saveTeam("  Los  Crácks "));

        assertThat(result.name()).isEqualTo("Los Crácks");
        assertThat(result.captainId()).isEqualTo(1L);
        var captor = ArgumentCaptor.forClass(TeamMembershipEntity.class);
        verify(memberships).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("CAPTAIN");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsTeamCreationByNonPlayer() {
        var courtAdmin = new AuthenticatedUser(9L, "court@example.com", Role.ADMIN_CANCHA, Plan.CANCHA_EMPRENDEDOR);

        assertThatThrownBy(() -> service.create(courtAdmin, saveTeam("Equipo Norte")))
                .isInstanceOf(ForbiddenActionException.class);
        verify(teams, never()).save(any());
    }

    @Test
    void invitesPlayerAndPreventsDuplicatePendingOperationUsingTeamLock() {
        var team = team(10L, 1L, "Fútbol");
        when(teams.findById(10L)).thenReturn(Optional.of(team));
        when(teams.findLockedById(10L)).thenReturn(Optional.of(team));
        when(users.findById(2L)).thenReturn(Optional.of(player(2L, "Luis")));
        when(memberships.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(requests.findFirstByTeamIdAndUserIdAndTypeAndStatus(10L, 2L, "INVITATION", "PENDING"))
                .thenReturn(Optional.empty(), Optional.of(request(20L, 10L, 2L, "INVITATION")));
        when(requests.findFirstByTeamIdAndUserIdAndTypeAndStatus(10L, 2L, "APPLICATION", "PENDING"))
                .thenReturn(Optional.empty());

        var invitation = service.invite(auth(1L), 10L, 2L);
        assertThat(invitation.status()).isEqualTo("PENDING");
        assertThatThrownBy(() -> service.invite(auth(1L), 10L, 2L))
                .isInstanceOf(ResourceConflictException.class);
        verify(teams, times(2)).findLockedById(10L);
    }

    @Test
    void acceptsAndRejectsInvitationsWithoutAddingRejectedPlayer() {
        var team = team(10L, 1L, "Fútbol");
        var accepted = request(20L, 10L, 2L, "INVITATION");
        when(requests.findLockedById(20L)).thenReturn(Optional.of(accepted));
        when(teams.findLockedById(10L)).thenReturn(Optional.of(team));
        when(teams.findById(10L)).thenReturn(Optional.of(team));
        when(memberships.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(memberships.countByTeamIdAndStatus(10L, "ACTIVE")).thenReturn(1L);
        when(users.findById(2L)).thenReturn(Optional.of(player(2L, "Luis")));

        var acceptedResult = service.respondInvitation(auth(2L), 20L, true);
        assertThat(acceptedResult.status()).isEqualTo("ACCEPTED");
        verify(memberships).save(any());

        var rejected = request(21L, 10L, 3L, "INVITATION");
        when(requests.findLockedById(21L)).thenReturn(Optional.of(rejected));
        when(users.findById(3L)).thenReturn(Optional.of(player(3L, "María")));
        var rejectedResult = service.respondInvitation(auth(3L), 21L, false);
        assertThat(rejectedResult.status()).isEqualTo("REJECTED");
    }

    @Test
    void captainApprovesApplicationAndCanRemoveNewMember() {
        var team = team(10L, 1L, "Fútbol");
        var application = request(20L, 10L, 2L, "APPLICATION");
        when(requests.findLockedById(20L)).thenReturn(Optional.of(application));
        when(teams.findById(10L)).thenReturn(Optional.of(team));
        when(teams.findLockedById(10L)).thenReturn(Optional.of(team));
        when(memberships.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(membership(10L, 2L, "MEMBER")));
        when(memberships.countByTeamIdAndStatus(10L, "ACTIVE")).thenReturn(1L);
        when(users.findById(2L)).thenReturn(Optional.of(player(2L, "Luis")));

        assertThat(service.respondApplication(auth(1L), 20L, true).status()).isEqualTo("APPROVED");
        service.removeMember(auth(1L), 10L, 2L);

        var captor = ArgumentCaptor.forClass(TeamMembershipEntity.class);
        verify(memberships, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(member -> "REMOVED".equals(member.getStatus()));
    }

    @Test
    void captainMustTransferLeadershipBeforeLeaving() {
        var team = team(10L, 1L, "Fútbol");
        when(teams.findById(10L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.leave(auth(1L), 10L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("capitanía");
    }

    @Test
    void transfersCaptaincyOnlyToActiveMember() {
        var team = team(10L, 1L, "Fútbol");
        var oldCaptain = membership(10L, 1L, "CAPTAIN");
        var nextCaptain = membership(10L, 2L, "MEMBER");
        when(teams.findLockedById(10L)).thenReturn(Optional.of(team));
        when(memberships.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(nextCaptain));
        when(memberships.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(oldCaptain));
        when(users.findById(2L)).thenReturn(Optional.of(player(2L, "Luis")));
        when(memberships.countByTeamIdAndStatus(10L, "ACTIVE")).thenReturn(2L);

        var result = service.transfer(auth(1L), 10L, 2L);

        assertThat(result.captainId()).isEqualTo(2L);
        assertThat(oldCaptain.getRole()).isEqualTo("MEMBER");
        assertThat(nextCaptain.getRole()).isEqualTo("CAPTAIN");
    }

    @Test
    void onlyCaptainCanPerformAdministrativeActions() {
        when(teams.findById(10L)).thenReturn(Optional.of(team(10L, 1L, "Fútbol")));

        assertThatThrownBy(() -> service.pendingRequests(auth(2L), 10L))
                .isInstanceOf(ForbiddenActionException.class);
        verify(requests, never()).findByTeamIdAndTypeAndStatusOrderByCreatedAt(any(), any(), any());
    }

    @Test
    void createsCompatibleChallengeButRejectsSelfAndDifferentSports() {
        var football = team(10L, 1L, "Fútbol");
        var opponent = team(11L, 2L, "Fútbol");
        when(teams.findLockedById(10L)).thenReturn(Optional.of(football));
        when(teams.findById(10L)).thenReturn(Optional.of(football));
        when(teams.findById(11L)).thenReturn(Optional.of(opponent));
        when(challenges.findFirstByChallengerTeamIdAndChallengedTeamIdAndStatus(10L, 11L, "PENDING"))
                .thenReturn(Optional.empty());

        var challenge = service.challenge(auth(1L), 11L, new TeamResources.CreateChallenge(10L, "Jugamos el sábado"));
        assertThat(challenge.status()).isEqualTo("PENDING");
        assertThat(challenge.matchId()).isNull();

        assertThatThrownBy(() -> service.challenge(auth(1L), 10L, new TeamResources.CreateChallenge(10L, null)))
                .isInstanceOf(IllegalArgumentException.class);

        opponent.setSport("Básquet");
        assertThatThrownBy(() -> service.challenge(auth(1L), 11L, new TeamResources.CreateChallenge(10L, null)))
                .isInstanceOf(ResourceConflictException.class);
    }

    private static AuthenticatedUser auth(Long id) {
        return new AuthenticatedUser(id, "player" + id + "@example.com", Role.JUGADOR, Plan.JUGADOR_BASICO);
    }

    private static TeamResources.SaveTeam saveTeam(String name) {
        return new TeamResources.SaveTeam(name, "Fútbol", "Equipo para jugar cada semana", "Miraflores", "INTERMEDIATE", true);
    }

    private static TeamEntity team(Long id, Long captainId, String sport) {
        var team = new TeamEntity();
        team.setId(id);
        team.setName("Equipo " + id);
        team.setNormalizedName("equipo " + id);
        team.setSport(sport);
        team.setDescription("Equipo deportivo permanente");
        team.setCompetitiveLevel("INTERMEDIATE");
        team.setOpenToRequests(true);
        team.setCaptainId(captainId);
        team.setStatus("ACTIVE");
        team.setMaxMembers(30);
        return team;
    }

    private static TeamMembershipEntity membership(Long teamId, Long userId, String role) {
        var membership = new TeamMembershipEntity();
        membership.setTeamId(teamId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setStatus("ACTIVE");
        membership.setJoinedAt(java.time.LocalDateTime.now());
        return membership;
    }

    private static TeamRequestEntity request(Long id, Long teamId, Long userId, String type) {
        var request = new TeamRequestEntity();
        request.setId(id);
        request.setTeamId(teamId);
        request.setUserId(userId);
        request.setRequestedBy(type.equals("INVITATION") ? 1L : userId);
        request.setType(type);
        request.setStatus("PENDING");
        return request;
    }

    private static UserPersistenceEntity player(Long id, String name) {
        var user = new UserPersistenceEntity();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setPassword("encoded");
        user.setRole(Role.JUGADOR.name());
        user.setPlan(Plan.JUGADOR_BASICO.name());
        user.setStatus("ACTIVE");
        return user;
    }
}
