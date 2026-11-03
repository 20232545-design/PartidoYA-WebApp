package com.partidoya.platform.auth.models;

import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.RawPassword;
import com.partidoya.platform.users.models.Role;

import java.util.List;
import java.util.Objects;

public record RegisterUserCommand(
        EmailAddress email,
        FullName fullName,
        String firstName,
        String lastName,
        RawPassword password,
        Role role,
        Plan requestedPlan,
        String phone,
        String district,
        String primarySport,
        String skillLevel,
        List<String> availability,
        String businessName,
        String businessPhone,
        String operationDistrict,
        boolean termsAccepted,
        boolean privacyAccepted,
        boolean marketingConsent) {
    public RegisterUserCommand {
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        Objects.requireNonNull(role, "role cannot be null");
        Objects.requireNonNull(requestedPlan, "requestedPlan cannot be null");
        availability = availability == null ? List.of() : List.copyOf(availability);
    }
}
