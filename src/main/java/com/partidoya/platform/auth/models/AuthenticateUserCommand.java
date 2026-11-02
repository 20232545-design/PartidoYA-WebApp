package com.partidoya.platform.auth.models;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.RawPassword;

import java.util.Objects;

public record AuthenticateUserCommand(
        EmailAddress email,
        RawPassword password) {
    public AuthenticateUserCommand {
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
    }
}
