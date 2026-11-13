package com.partidoya.platform.chat.controllers;

import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.chat.dto.ChatMessageResource;
import com.partidoya.platform.chat.dto.PostChatMessageResource;
import com.partidoya.platform.chat.services.MatchChatService;
import com.partidoya.platform.matches.models.MatchId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Historial y envío de mensajes. Los mensajes nuevos se difunden a los suscriptores de
 * {@code /topic/matches/{matchId}/chat}; el autor siempre se toma del token.
 */
@RestController
@RequestMapping(value = "/api/v1/matches/{matchId}/messages", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Match Chat", description = "Group chat between the players of a match")
public class MatchChatController {
    private final MatchChatService chatService;
    private final CurrentUserService currentUserService;

    public MatchChatController(MatchChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResource>> history(
            @PathVariable Long matchId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Integer limit) {
        var messages = chatService.history(new MatchId(matchId), currentUserService.userId(), before, limit);
        return ResponseEntity.ok(messages.stream().map(ChatMessageResource::from).toList());
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatMessageResource> post(@PathVariable Long matchId,
                                                    @RequestBody PostChatMessageResource resource) {
        var message = chatService.post(new MatchId(matchId), currentUserService.userId(), resource.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatMessageResource.from(message));
    }
}
