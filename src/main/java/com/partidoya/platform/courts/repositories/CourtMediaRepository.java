package com.partidoya.platform.courts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourtMediaRepository extends JpaRepository<CourtMediaEntity, Long> {
    List<CourtMediaEntity> findByCourtIdOrderByDisplayOrderAsc(Long courtId);
    Optional<CourtMediaEntity> findFirstByCourtIdAndPrimaryAssetTrue(Long courtId);
}
