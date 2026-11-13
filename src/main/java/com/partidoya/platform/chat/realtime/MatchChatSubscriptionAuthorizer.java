package com.partidoya.platform.chat.realtime;

import com.partidoya.platform.chat.services.MatchChatService;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.realtime.SubscriptionAuthorizer;
import com.partidoya.platform.users.models.UserId;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Solo los miembros del partido pueden escuchar {@code /topic/matches/{matchId}/chat}. */
@Component
public class MatchChatSubscriptionAuthorizer implements SubscriptionAuthorizer {
    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/matches/(\\d{1,18})/chat$");

    private final MatchChatService chatService;

    public MatchChatSubscriptionAuthorizer(MatchChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean supports(String destination) {
        return CHAT_TOPIC.matcher(destination).matches();
    }

    @Override
    public void authorize(UserId userId, String destination) {
        var matcher = CHAT_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported destination");
        }
        chatService.requireReadAccess(new MatchId(Long.parseLong(matcher.group(1))), userId);
    }
}
