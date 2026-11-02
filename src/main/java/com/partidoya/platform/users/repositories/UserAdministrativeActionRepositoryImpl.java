package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.UserAdministrativeAction;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserAdministrativeActionRepository;
import com.partidoya.platform.users.repositories.UserAdministrativeActionPersistenceAssembler;
import com.partidoya.platform.users.repositories.UserAdministrativeActionPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserAdministrativeActionRepositoryImpl implements UserAdministrativeActionRepository {
    private final UserAdministrativeActionPersistenceRepository repository;

    public UserAdministrativeActionRepositoryImpl(UserAdministrativeActionPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAdministrativeAction save(UserAdministrativeAction action) {
        return UserAdministrativeActionPersistenceAssembler.toDomainFromPersistence(
                repository.save(UserAdministrativeActionPersistenceAssembler.toPersistenceFromDomain(action)));
    }

    @Override
    public List<UserAdministrativeAction> findByTargetUserId(UserId targetUserId) {
        return repository.findByTargetUserIdOrderByActionCreatedAtDesc(targetUserId.value()).stream()
                .map(UserAdministrativeActionPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
