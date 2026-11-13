package com.partidoya.platform.notifications.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 18, 0);
    private static final UserId RECIPIENT = new UserId(7L);
    private static final MatchId MATCH_ID = new MatchId(1L);

    @Test
    void markingAsReadKeepsTheFirstReadDate() {
        var notification = Notification.create(RECIPIENT, MATCH_ID, NotificationType.MATCH_FULL, "Completo", NOW);

        notification.markAsRead(NOW.plusMinutes(1));
        notification.markAsRead(NOW.plusMinutes(5));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void firstChatActivityNamesTheAuthor() {
        var notification = Notification.chatActivity(RECIPIENT, MATCH_ID, "Pichanga", "Ana Torres", NOW);

        assertThat(notification.getType()).isEqualTo(NotificationType.CHAT_MESSAGES);
        assertThat(notification.getGroupedCount()).isEqualTo(1);
        assertThat(notification.getMessage()).isEqualTo("Ana Torres escribió en el chat de «Pichanga».");
    }

    @Test
    void unreadChatNotificationAccumulatesMessages() {
        var notification = Notification.chatActivity(RECIPIENT, MATCH_ID, "Pichanga", "Ana Torres", NOW);

        notification.registerChatMessage("Pichanga", "Luis", NOW.plusMinutes(1));
        notification.registerChatMessage("Pichanga", "Ana Torres", NOW.plusMinutes(2));

        assertThat(notification.getGroupedCount()).isEqualTo(3);
        assertThat(notification.getMessage()).isEqualTo("3 mensajes nuevos en el chat de «Pichanga».");
        assertThat(notification.getOccurredAt()).isEqualTo(NOW.plusMinutes(2));
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void readChatNotificationStartsCountingAgainAndBecomesUnread() {
        var notification = Notification.chatActivity(RECIPIENT, MATCH_ID, "Pichanga", "Ana Torres", NOW);
        notification.registerChatMessage("Pichanga", "Luis", NOW.plusMinutes(1));
        notification.markAsRead(NOW.plusMinutes(2));

        notification.registerChatMessage("Pichanga", "Luis", NOW.plusMinutes(3));

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getGroupedCount()).isEqualTo(1);
        assertThat(notification.getMessage()).isEqualTo("Luis escribió en el chat de «Pichanga».");
    }

    @Test
    void onlyChatNotificationsCanGroupMessages() {
        var notification = Notification.create(RECIPIENT, MATCH_ID, NotificationType.MATCH_FULL, "Completo", NOW);

        assertThatThrownBy(() -> notification.registerChatMessage("Pichanga", "Luis", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsBlankMessages() {
        assertThatThrownBy(() -> Notification.create(RECIPIENT, MATCH_ID, NotificationType.MATCH_FULL, "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMessagesLongerThanTheLimit() {
        var tooLong = "x".repeat(Notification.MAX_MESSAGE_LENGTH + 1);

        assertThatThrownBy(() -> Notification.create(RECIPIENT, MATCH_ID, NotificationType.MATCH_FULL, tooLong, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
