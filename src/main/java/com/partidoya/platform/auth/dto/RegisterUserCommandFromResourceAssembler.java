package com.partidoya.platform.auth.dto;

import com.partidoya.platform.auth.models.RegisterUserCommand;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.auth.dto.RegisterUserResource;

public final class RegisterUserCommandFromResourceAssembler {
    private RegisterUserCommandFromResourceAssembler() {
    }

    public static RegisterUserCommand toCommandFromResource(RegisterUserResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Registration data is required");
        }
        var role = parsePublicRole(resource.role());
        var requestedPlan = parsePlan(resource.planCode(), role);
        var firstName = required(resource.firstName(), "First name");
        var lastName = required(resource.lastName(), "Last name");
        return new RegisterUserCommand(
                new EmailAddress(resource.email()),
                new FullName(firstName + " " + lastName),
                firstName,
                lastName,
                new RawPassword(resource.password()),
                role,
                requestedPlan,
                resource.phone(),
                resource.district(),
                resource.primarySport(),
                resource.skillLevel(),
                resource.availability(),
                resource.businessName(),
                resource.businessPhone(),
                resource.operationDistrict(),
                resource.termsAccepted(),
                resource.privacyAccepted(),
                resource.marketingConsent());
    }

    private static Role parsePublicRole(String value) {
        try {
            var role = Role.valueOf(value == null ? "" : value.trim().toUpperCase());
            if (role == Role.ADMIN_GENERAL) {
                throw new IllegalArgumentException();
            }
            return role;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Account type must be JUGADOR or ADMIN_CANCHA");
        }
    }

    private static Plan parsePlan(String value, Role role) {
        try {
            var plan = Plan.valueOf(value == null ? "" : value.trim().toUpperCase());
            if (plan == Plan.ADMIN_GENERAL || !plan.isCompatibleWith(role)) {
                throw new IllegalArgumentException();
            }
            return plan;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Selected plan is not available for this account type");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
