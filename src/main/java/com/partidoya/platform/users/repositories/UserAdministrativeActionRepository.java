package com.partidoya.platform.users.repositories;

import com.partidoya.platform.users.models.UserAdministrativeAction;
import com.partidoya.platform.users.models.UserId;

import java.util.List;

public interface UserAdministrativeActionRepository {
    UserAdministrativeAction save(UserAdministrativeAction action);
    List<UserAdministrativeAction> findByTargetUserId(UserId targetUserId);
}
