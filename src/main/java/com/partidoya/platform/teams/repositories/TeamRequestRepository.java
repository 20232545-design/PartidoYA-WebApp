package com.partidoya.platform.teams.repositories;

import com.partidoya.platform.teams.models.TeamRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface TeamRequestRepository extends JpaRepository<TeamRequestEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TeamRequestEntity r where r.id = :id")
    Optional<TeamRequestEntity> findLockedById(@Param("id") Long id);
    Optional<TeamRequestEntity> findFirstByTeamIdAndUserIdAndTypeAndStatus(Long teamId, Long userId, String type, String status);
    List<TeamRequestEntity> findByTeamIdAndTypeAndStatusOrderByCreatedAt(Long teamId, String type, String status);
    List<TeamRequestEntity> findByTeamIdAndStatusOrderByCreatedAt(Long teamId, String status);
    List<TeamRequestEntity> findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(Long userId, String type, String status);
    List<TeamRequestEntity> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
    List<TeamRequestEntity> findByTeamIdInAndUserIdAndTypeAndStatus(List<Long> teamIds, Long userId, String type, String status);
    long countByTeamIdAndTypeAndStatus(Long teamId, String type, String status);
}
