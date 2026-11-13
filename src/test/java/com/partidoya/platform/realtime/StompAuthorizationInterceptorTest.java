package com.partidoya.platform.realtime;

import com.partidoya.platform.users.models.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StompAuthorizationInterceptorTest {
    private static final StompUser USER = new StompUser(new UserId(7L));

    private final RecordingAuthorizer chatAuthorizer = new RecordingAuthorizer("/topic/chat");
    private final StompAuthorizationInterceptor interceptor = new StompAuthorizationInterceptor(List.of(chatAuthorizer));

    @Test
    void delegatesSubscriptionsToTheAuthorizerThatSupportsTheDestination() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat", USER), null);

        assertThat(chatAuthorizer.authorized).containsExactly(USER.userId());
    }

    @Test
    void rejectsSubscriptionsToUnknownDestinations() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/other", USER), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThat(chatAuthorizer.authorized).isEmpty();
    }

    @Test
    void rejectsSubscriptionsWithoutAnAuthenticatedUser() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat", null), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void turnsTheAuthorizerDenialIntoAGenericStompError() {
        chatAuthorizer.deny = true;

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat", USER), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("Subscription denied")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsClientSendFramesBecauseMessagesGoThroughRest() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, "/topic/chat", USER), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void letsOtherFramesThrough() {
        var disconnect = frame(StompCommand.DISCONNECT, null, USER);

        assertThat(interceptor.preSend(disconnect, null)).isSameAs(disconnect);
    }

    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        var accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static final class RecordingAuthorizer implements SubscriptionAuthorizer {
        private final String destination;
        private final List<UserId> authorized = new ArrayList<>();
        private boolean deny;

        RecordingAuthorizer(String destination) {
            this.destination = destination;
        }

        @Override
        public boolean supports(String candidate) {
            return destination.equals(candidate);
        }

        @Override
        public void authorize(UserId userId, String candidate) {
            if (deny) throw new IllegalStateException("denied");
            authorized.add(userId);
        }
    }
}
