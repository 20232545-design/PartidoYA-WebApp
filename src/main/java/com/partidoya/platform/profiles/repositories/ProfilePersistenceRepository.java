package com.partidoya.platform.profiles.repositories;

import com.partidoya.platform.profiles.repositories.ProfilePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilePersistenceRepository extends JpaRepository<ProfilePersistenceEntity, Long> {
    Optional<ProfilePersistenceEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
