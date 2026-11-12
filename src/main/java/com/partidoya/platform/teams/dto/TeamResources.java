package com.partidoya.platform.teams.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public final class TeamResources {
    private TeamResources() {}

    public record SaveTeam(String name, String sport, String description, String district,
                           String competitiveLevel, boolean openToRequests) {}
    public record Team(Long id, String name, String sport, String description, String logoUrl,
                       String district, String competitiveLevel, boolean openToRequests,
                       String status, Long captainId, String captainName, long memberCount,
                       Date createdAt, boolean member, boolean captain, boolean pendingRequest,
                       TeamStatistics statistics) {}
    public record TeamStatistics(boolean available, int played, int wins, int draws, int losses, double winPercentage) {}
    public record TeamPage(List<Team> content, int page, int size, long totalElements, int totalPages) {}
    public record Member(Long userId, String publicName, String avatarUrl, String role,
                         String primarySport, String skillLevel, LocalDateTime joinedAt) {}
    public record Player(Long userId, String publicName, String avatarUrl, String skillLevel) {}
    public record PublicPlayer(Long userId, String publicName, String avatarUrl, String district,
                               String primarySport, String skillLevel) {}
    public record TeamRequest(Long id, Long teamId, String teamName, Long userId, String userName,
                              String type, String status, Date createdAt, LocalDateTime expiresAt) {}
    public record InvitePlayer(Long userId) {}
    public record TransferCaptain(Long userId) {}
    public record CreateChallenge(Long challengerTeamId, String message) {}
    public record Challenge(Long id, Long challengerTeamId, String challengerTeamName,
                            Long challengedTeamId, String challengedTeamName, String sport,
                            String message, String status, Long matchId, Date createdAt) {}
    public record MyTeams(List<Team> administered, List<Team> memberships,
                          List<TeamRequest> invitations, List<Challenge> challenges, long pendingCount) {}
    public record Logo(String logoUrl) {}
}
