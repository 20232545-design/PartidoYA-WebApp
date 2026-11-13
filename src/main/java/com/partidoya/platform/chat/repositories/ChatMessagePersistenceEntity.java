package com.partidoya.platform.chat.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessagePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
