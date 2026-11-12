package com.partidoya.platform.users.services;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.auth.security.PasswordHasher;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return userRepository.findById(userId);
    }

    /** Nombres visibles de varios usuarios a la vez, indexados por id. */
    @Transactional(readOnly = true)
    public Map<Long, String> findDisplayNames(Collection<UserId> userIds) {
        var names = new HashMap<Long, String>();
        for (var user : userRepository.findAllByIds(userIds)) {
            names.put(user.getId().value(), user.getFullName().value());
        }
        return names;
    }

    public User updateFullName(UserId userId, FullName fullName) {
        var user = requireUser(userId);
        user.updateFullName(fullName);
        return userRepository.save(user);
    }

    public void changePassword(UserId userId, String currentPassword, String newPassword) {
        var user = requireUser(userId);
        if (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPassword().value())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        validatePasswordStrength(newPassword);
        if (passwordHasher.matches(newPassword, user.getPassword().value())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }
        user.changePassword(new HashedPassword(passwordHasher.hash(newPassword)));
        userRepository.save(user);
    }

    private static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 10
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 10 caracteres, mayúscula, minúscula, número y símbolo");
        }
    }

    private User requireUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
    }
}
