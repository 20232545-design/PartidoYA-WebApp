package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.users.repositories.UserPersistenceAssembler;
import com.partidoya.platform.users.repositories.UserPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserPersistenceRepository userPersistenceRepository;

    public UserRepositoryImpl(UserPersistenceRepository userPersistenceRepository) {
        this.userPersistenceRepository = userPersistenceRepository;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userPersistenceRepository.findById(id.value())
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<User> findAllByIds(Collection<UserId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        var rawIds = ids.stream().map(UserId::value).distinct().toList();
        return userPersistenceRepository.findAllById(rawIds).stream()
                .map(UserPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return userPersistenceRepository.findByEmail(email.value())
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<User> search(String search, Role role, int page, int size) {
        var normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        var normalizedRole = role == null ? null : role.name();
        return userPersistenceRepository.search(normalizedSearch, normalizedRole, PageRequest.of(page, size)).stream()
                .map(UserPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public long countSearch(String search, Role role) {
        var normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        var normalizedRole = role == null ? null : role.name();
        return userPersistenceRepository.countSearch(normalizedSearch, normalizedRole);
    }

    @Override
    public User save(User user) {
        var entity = UserPersistenceAssembler.toPersistenceFromDomain(user);
        var saved = userPersistenceRepository.save(entity);
        return UserPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return userPersistenceRepository.existsByEmail(email.value());
    }
}
