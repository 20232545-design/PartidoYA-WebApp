package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.User;
import com.partidoya.platform.users.models.EmailAddress;
import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.HashedPassword;
import com.partidoya.platform.users.models.Plan;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.models.UserStatus;
import com.partidoya.platform.users.repositories.UserPersistenceEntity;

public final class UserPersistenceAssembler {

    private UserPersistenceAssembler() {
    }

    public static User toDomainFromPersistence(UserPersistenceEntity entity) {
        if (entity == null) return null;
        return new User(
                new UserId(entity.getId()),
                new EmailAddress(entity.getEmail()),
                new FullName(entity.getFullName()),
                new HashedPassword(entity.getPassword()),
                entity.isEmailVerified(),
                Role.valueOf(entity.getRole()),
                Plan.valueOf(entity.getPlan()),
                entity.getStatus() == null || entity.getStatus().isBlank() ? UserStatus.ACTIVE : UserStatus.valueOf(entity.getStatus()),
                entity.getSuspensionReason(),
                entity.getSuspendedUntil(),
                entity.getLastAdministrativeActionAt(),
                entity.getLastAdministrativeActionBy() == null ? null : new UserId(entity.getLastAdministrativeActionBy()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getTermsAcceptedAt(),
                entity.getPrivacyAcceptedAt(),
                entity.isMarketingConsent(),
                entity.isLegacyPlanAccess());
    }

    public static UserPersistenceEntity toPersistenceFromDomain(User user) {
        if (user == null) return null;
        var entity = new UserPersistenceEntity();
        if (user.getId() != null) {
            entity.setId(user.getId().value());
        }
        entity.setEmail(user.getEmail().value());
        entity.setFullName(user.getFullName().value());
        entity.setPassword(user.getPassword().value());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setRole(user.getRole().name());
        entity.setPlan(user.getPlan().name());
        entity.setStatus(user.getStatus().name());
        entity.setSuspensionReason(user.getSuspensionReason());
        entity.setSuspendedUntil(user.getSuspendedUntil());
        entity.setLastAdministrativeActionAt(user.getLastAdministrativeActionAt());
        entity.setLastAdministrativeActionBy(user.getLastAdministrativeActionBy() == null ? null : user.getLastAdministrativeActionBy().value());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setTermsAcceptedAt(user.getTermsAcceptedAt());
        entity.setPrivacyAcceptedAt(user.getPrivacyAcceptedAt());
        entity.setMarketingConsent(user.isMarketingConsent());
        entity.setLegacyPlanAccess(user.isLegacyPlanAccess());
        return entity;
    }
}
