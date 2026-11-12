package com.partidoya.platform.teams.repositories;

import com.partidoya.platform.teams.models.TeamMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamMembershipRepository extends JpaRepository<TeamMembershipEntity, Long> {
    Optional<TeamMembershipEntity> findByTeamIdAndUserId(Long teamId, Long userId);
    List<TeamMembershipEntity> findByTeamIdAndStatusOrderByJoinedAt(Long teamId, String status);
    List<TeamMembershipEntity> findByUserIdAndStatus(Long userId, String status);
    List<TeamMembershipEntity> findByTeamIdInAndStatus(List<Long> teamIds, String status);
    long countByTeamIdAndStatus(Long teamId, String status);
}
