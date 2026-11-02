package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.courts.models.Court;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtRepository;
import com.partidoya.platform.courts.repositories.CourtPersistenceAssembler;
import com.partidoya.platform.courts.repositories.CourtPersistenceRepository;
import com.partidoya.platform.iam.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CourtRepositoryImpl implements CourtRepository {
    private final CourtPersistenceRepository courtPersistenceRepository;

    public CourtRepositoryImpl(CourtPersistenceRepository courtPersistenceRepository) {
        this.courtPersistenceRepository = courtPersistenceRepository;
    }

    @Override
    public Optional<Court> findById(CourtId id) {
        return courtPersistenceRepository.findById(id.value()).map(CourtPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Court> findPublished(String sport, String district) {
        var normalizedSport = sport == null || sport.isBlank() ? null : sport.trim();
        var normalizedDistrict = district == null || district.isBlank() ? null : district.trim();
        return courtPersistenceRepository.findPublished(normalizedSport, normalizedDistrict).stream()
                .map(CourtPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Court> findByOwnerId(UserId ownerId) {
        return courtPersistenceRepository.findByOwnerId(ownerId.value()).stream()
                .map(CourtPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    @Transactional
    public Court save(Court court) {
        var entity = CourtPersistenceAssembler.toPersistenceFromDomain(court);
        return CourtPersistenceAssembler.toDomainFromPersistence(courtPersistenceRepository.save(entity));
    }
}
