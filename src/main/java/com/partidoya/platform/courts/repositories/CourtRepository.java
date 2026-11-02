package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.users.models.UserId;

import java.util.List;
import java.util.Optional;

public interface CourtRepository {
    Optional<Court> findById(CourtId id);
    List<Court> findPublished(String sport, String district);
    List<Court> findByOwnerId(UserId ownerId);
    Court save(Court court);
}
