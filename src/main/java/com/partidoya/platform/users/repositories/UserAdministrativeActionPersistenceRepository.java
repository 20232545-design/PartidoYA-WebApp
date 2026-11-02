package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.repositories.UserAdministrativeActionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAdministrativeActionPersistenceRepository extends JpaRepository<UserAdministrativeActionPersistenceEntity, Long> {
    List<UserAdministrativeActionPersistenceEntity> findByTargetUserIdOrderByActionCreatedAtDesc(Long targetUserId);
}
