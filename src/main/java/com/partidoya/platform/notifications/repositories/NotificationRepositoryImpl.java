package com.partidoya.platform.notifications.repositories;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.notifications.models.Notification;
import com.partidoya.platform.notifications.models.NotificationType;
import com.partidoya.platform.users.models.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationPersistenceRepository repository;

    public NotificationRepositoryImpl(NotificationPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = NotificationPersistenceAssembler.toPersistenceFromDomain(notification);
        return NotificationPersistenceAssembler.toDomainFromPersistence(repository.save(entity));
    }

    @Override
    public Optional<Notification> findByIdAndRecipientId(Long id, UserId recipientId) {
        return repository.findByIdAndRecipientId(id, recipientId.value())
                .map(NotificationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Notification> findLatest(UserId recipientId, MatchId matchId, NotificationType type) {
        return repository.findFirstByRecipientIdAndMatchIdAndTypeOrderByIdDesc(
                        recipientId.value(), matchId.value(), type.name())
                .map(NotificationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Notification> findByRecipientId(UserId recipientId, int page, int size) {
        return repository.findByRecipientIdOrderByOccurredAtDescIdDesc(recipientId.value(), PageRequest.of(page, size))
                .stream()
                .map(NotificationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public long countUnreadByRecipientId(UserId recipientId) {
        return repository.countByRecipientIdAndReadAtIsNull(recipientId.value());
    }

    @Override
    public int markAllAsRead(UserId recipientId, LocalDateTime readAt) {
        return repository.markAllAsRead(recipientId.value(), readAt);
    }

    @Override
    public int markAsReadByMatchAndType(UserId recipientId, MatchId matchId, NotificationType type,
                                        LocalDateTime readAt) {
        return repository.markAsReadByMatchAndType(recipientId.value(), matchId.value(), type.name(), readAt);
    }
}
