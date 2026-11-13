package com.partidoya.platform.chat.models;

import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 18, 0);
    private static final MatchId MATCH_ID = new MatchId(1L);
    private static final UserId AUTHOR = new UserId(7L);

    @Test
    void trimsSurroundingWhitespaceButKeepsInnerLineBreaks() {
        var message = ChatMessage.post(MATCH_ID, AUTHOR, "  Llego en 10\nGuarden cupo  ", NOW);

        assertThat(message.getContent()).isEqualTo("Llego en 10\nGuarden cupo");
        assertThat(message.getSentAt()).isEqualTo(NOW);
        assertThat(message.getId()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\n\t"})
    void rejectsEmptyContent(String content) {
        assertThatThrownBy(() -> ChatMessage.post(MATCH_ID, AUTHOR, content, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsContentExactlyAtTheLimit() {
        var content = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH);

        assertThat(ChatMessage.post(MATCH_ID, AUTHOR, content, NOW).getContent()).hasSize(ChatMessage.MAX_CONTENT_LENGTH);
    }

    @Test
    void rejectsContentLongerThanTheLimit() {
        var content = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> ChatMessage.post(MATCH_ID, AUTHOR, content, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
