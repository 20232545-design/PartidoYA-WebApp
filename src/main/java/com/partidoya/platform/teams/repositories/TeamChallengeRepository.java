package com.partidoya.platform.teams.repositories;

import com.partidoya.platform.teams.models.TeamChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface TeamChallengeRepository extends JpaRepository<TeamChallengeEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TeamChallengeEntity c where c.id = :id")
    Optional<TeamChallengeEntity> findLockedById(@Param("id") Long id);
    Optional<TeamChallengeEntity> findFirstByChallengerTeamIdAndChallengedTeamIdAndStatus(Long challenger, Long challenged, String status);
    @Query("select c from TeamChallengeEntity c where c.challengerTeamId in :ids or c.challengedTeamId in :ids order by c.createdAt desc")
    List<TeamChallengeEntity> findForTeams(@Param("ids") List<Long> ids);
}
