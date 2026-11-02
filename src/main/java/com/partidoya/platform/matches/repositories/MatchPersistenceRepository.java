package com.partidoya.platform.matches.repositories;

import com.partidoya.platform.matches.repositories.MatchPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchPersistenceRepository extends JpaRepository<MatchPersistenceEntity, Long> {
    List<MatchPersistenceEntity> findAllByStatusOrderByMatchDateAsc(String status);
    List<MatchPersistenceEntity> findAllByStatusAndSportIgnoreCaseOrderByMatchDateAsc(String status, String sport);
    List<MatchPersistenceEntity> findAllByOrganizerId(Long organizerId);

    @Query("SELECT m FROM MatchPersistenceEntity m JOIN m.participantIds p WHERE p = :userId")
    List<MatchPersistenceEntity> findAllByParticipantId(@Param("userId") Long userId);

    @Query("""
            select count(distinct m) from MatchPersistenceEntity m
            left join m.participantIds p
            where m.organizerId = :userId or p = :userId
            """)
    long countRelatedToUser(@Param("userId") Long userId);
}
