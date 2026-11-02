package com.partidoya.platform.auth.dto;

import com.partidoya.platform.auth.models.AuthenticateUserCommand;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.auth.dto.LoginUserResource;

public class AuthenticateUserCommandFromResourceAssembler {
    public static AuthenticateUserCommand toCommandFromResource(LoginUserResource resource) {
        return new AuthenticateUserCommand(
                new EmailAddress(resource.email()),
                new RawPassword(resource.password()));
    }
}
