package com.partidoya.platform.auth.models;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.users.models.Role;

import java.util.Objects;

public record RegisterUserCommand(
        EmailAddress email,
        FullName fullName,
        RawPassword password,
        Role role) {
    public RegisterUserCommand {
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        Objects.requireNonNull(role, "role cannot be null");
    }
}
