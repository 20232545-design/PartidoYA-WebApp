package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CourtSearchService {
    private final CourtRepository courtRepository;
    private final CourtAccessPolicy accessPolicy;

    public CourtSearchService(CourtRepository courtRepository, CourtAccessPolicy accessPolicy) {
        this.courtRepository = courtRepository;
        this.accessPolicy = accessPolicy;
    }

    public List<Court> searchPublished(String sport, String district) {
        return courtRepository.findPublished(sport, district).stream()
                .filter(accessPolicy::isEligibleForReservations)
                .toList();
    }

    public Optional<Court> findPublishedById(CourtId courtId) {
        return courtRepository.findById(courtId).filter(accessPolicy::isEligibleForReservations);
    }
}
