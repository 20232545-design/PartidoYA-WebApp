package com.partidoya.platform.auth.dto;

import com.partidoya.platform.auth.models.RegisterUserCommand;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.auth.dto.RegisterUserResource;

public class RegisterUserCommandFromResourceAssembler {
    public static RegisterUserCommand toCommandFromResource(RegisterUserResource resource) {
        var role = resource.role() == null || resource.role().isBlank()
                ? Role.JUGADOR
                : Role.valueOf(resource.role());
        return new RegisterUserCommand(
                new EmailAddress(resource.email()),
                new FullName(resource.fullName()),
                new RawPassword(resource.password()),
                role);
    }
}
