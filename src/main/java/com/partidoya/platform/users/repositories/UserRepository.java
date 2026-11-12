package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(UserId id);
    List<User> findAllByIds(Collection<UserId> ids);
    Optional<User> findByEmail(EmailAddress email);
    List<User> search(String search, Role role, int page, int size);
    long countSearch(String search, Role role);
    User save(User user);
    boolean existsByEmail(EmailAddress email);
}
