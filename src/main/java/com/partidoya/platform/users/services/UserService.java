package com.partidoya.platform.users.services;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserRepository;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return userRepository.findById(userId);
    }

    public User updateFullName(UserId userId, FullName fullName) {
        var user = requireUser(userId);
        user.updateFullName(fullName);
        return userRepository.save(user);
    }

    public User updatePlan(UserId userId, Plan plan) {
        var user = requireUser(userId);
        user.updatePlan(plan);
        return userRepository.save(user);
    }

    private User requireUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.value().toString()));
    }
}
