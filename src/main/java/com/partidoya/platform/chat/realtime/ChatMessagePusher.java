package com.partidoya.platform.chat.realtime;

import com.partidoya.platform.chat.dto.ChatMessageResource;
import com.partidoya.platform.chat.models.ChatMessagePosted;
import com.partidoya.platform.realtime.RealtimeDestinations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Difunde cada mensaje a quienes tienen el chat abierto, recién cuando ya quedó guardado. */
@Component
public class ChatMessagePusher {
    private static final Logger log = LoggerFactory.getLogger(ChatMessagePusher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessagePusher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onMessagePosted(ChatMessagePosted event) {
        var message = event.view().message();
        try {
            messagingTemplate.convertAndSend(
                    RealtimeDestinations.matchChatTopic(message.getMatchId()),
                    ChatMessageResource.from(event.view()));
        } catch (MessagingException pushFailure) {
            // El mensaje ya está guardado; los clientes lo recuperan al recargar el historial.
            log.warn("Could not broadcast chat message {}", message.getId(), pushFailure);
        }
    }
}
