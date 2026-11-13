package com.partidoya.platform.support;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake en memoria de {@link UserRepository} con un atajo para registrar jugadores por nombre. */
public final class InMemoryUserRepository implements UserRepository {
    private final Map<UserId, User> users = new HashMap<>();

    public InMemoryUserRepository withPlayer(UserId id, String fullName) {
        users.put(id, new User(id, new EmailAddress("player" + id.value() + "@test.com"), new FullName(fullName),
                new HashedPassword("hashed-password"), true, Role.JUGADOR, Plan.JUGADOR_BASICO));
        return this;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAllByIds(Collection<UserId> ids) {
        return ids.stream().distinct().map(users::get).filter(user -> user != null).toList();
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public List<User> search(String search, Role role, int page, int size) {
        throw new UnsupportedOperationException("Not needed by these tests");
    }

    @Override
    public long countSearch(String search, Role role) {
        throw new UnsupportedOperationException("Not needed by these tests");
    }

    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return findByEmail(email).isPresent();
    }
}
