package com.partidoya.platform.notifications.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationPersistenceRepository extends JpaRepository<NotificationPersistenceEntity, Long> {
    Optional<NotificationPersistenceEntity> findByIdAndRecipientId(Long id, Long recipientId);

    Optional<NotificationPersistenceEntity> findFirstByRecipientIdAndMatchIdAndTypeOrderByIdDesc(
            Long recipientId, Long matchId, String type);

    List<NotificationPersistenceEntity> findByRecipientIdOrderByOccurredAtDescIdDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationPersistenceEntity n
               set n.readAt = :readAt, n.updatedAt = current_timestamp
             where n.recipientId = :recipientId and n.readAt is null
            """)
    int markAllAsRead(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationPersistenceEntity n
               set n.readAt = :readAt, n.updatedAt = current_timestamp
             where n.recipientId = :recipientId and n.matchId = :matchId and n.type = :type and n.readAt is null
            """)
    int markAsReadByMatchAndType(@Param("recipientId") Long recipientId, @Param("matchId") Long matchId,
                                 @Param("type") String type, @Param("readAt") LocalDateTime readAt);
}
