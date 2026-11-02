package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.UserAdministrativeAction;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.repositories.UserAdministrativeActionPersistenceEntity;

public final class UserAdministrativeActionPersistenceAssembler {
    private UserAdministrativeActionPersistenceAssembler() {
    }

    public static UserAdministrativeAction toDomainFromPersistence(UserAdministrativeActionPersistenceEntity entity) {
        if (entity == null) return null;
        return new UserAdministrativeAction(
                entity.getId(),
                new UserId(entity.getAdminId()),
                new UserId(entity.getTargetUserId()),
                entity.getAction(),
                entity.getReason(),
                entity.getActionCreatedAt());
    }

    public static UserAdministrativeActionPersistenceEntity toPersistenceFromDomain(UserAdministrativeAction action) {
        if (action == null) return null;
        var entity = new UserAdministrativeActionPersistenceEntity();
        if (action.getId() != null) entity.setId(action.getId());
        entity.setAdminId(action.getAdminId().value());
        entity.setTargetUserId(action.getTargetUserId().value());
        entity.setAction(action.getAction());
        entity.setReason(action.getReason());
        entity.setActionCreatedAt(action.getCreatedAt());
        return entity;
    }
}
