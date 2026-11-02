package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.repositories.CourtPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourtPersistenceRepository extends JpaRepository<CourtPersistenceEntity, Long> {
    List<CourtPersistenceEntity> findByOwnerId(Long ownerId);

    @Query("""
            select distinct c from CourtPersistenceEntity c
            left join c.sports s
            where c.active = true
              and c.published = true
              and c.availableForReservations = true
              and (:sport is null or lower(s) = lower(:sport))
              and (:district is null or lower(c.district) like lower(concat('%', :district, '%')))
            """)
    List<CourtPersistenceEntity> findPublished(@Param("sport") String sport, @Param("district") String district);
}
