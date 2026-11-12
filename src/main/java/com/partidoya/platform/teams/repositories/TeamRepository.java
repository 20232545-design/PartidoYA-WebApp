package com.partidoya.platform.teams.repositories;

import com.partidoya.platform.teams.models.TeamEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    boolean existsByNormalizedName(String normalizedName);
    @Query("""
        select t from TeamEntity t where t.status = 'ACTIVE'
        and (:search is null or lower(t.name) like lower(concat('%', :search, '%')))
        and (:sport is null or t.sport = :sport)
        and (:district is null or lower(t.district) = lower(:district))
        and (:level is null or t.competitiveLevel = :level)
        and (:open is null or t.openToRequests = :open)
        """)
    Page<TeamEntity> search(@Param("search") String search, @Param("sport") String sport,
                            @Param("district") String district, @Param("level") String level,
                            @Param("open") Boolean open, Pageable pageable);
    List<TeamEntity> findByCaptainIdAndStatusOrderByName(Long captainId, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TeamEntity t where t.id = :id")
    Optional<TeamEntity> findLockedById(@Param("id") Long id);
}
