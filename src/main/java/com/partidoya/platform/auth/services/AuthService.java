package com.partidoya.platform.auth.services;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.auth.models.AuthenticateUserCommand;
import com.partidoya.platform.auth.models.RegisterUserCommand;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.auth.security.PasswordHasher;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(RegisterUserCommand request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceConflictException(
                    "User",
                    "Email '%s' is already registered".formatted(request.email().value()));
        }
        var password = new HashedPassword(passwordHasher.hash(request.password().value()));
        return userRepository.save(new User(request.email(), request.fullName(), password, request.role()));
    }

    @Transactional(readOnly = true)
    public User authenticate(AuthenticateUserCommand request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email().value()));
        if (!passwordHasher.matches(request.password().value(), user.getPassword().value())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!user.isActive()) {
            throw new ForbiddenActionException("Account is suspended");
        }
        return user;
    }
}
