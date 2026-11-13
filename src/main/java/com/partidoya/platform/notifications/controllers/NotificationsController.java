package com.partidoya.platform.notifications.controllers;

import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.notifications.dto.NotificationPageResource;
import com.partidoya.platform.notifications.dto.NotificationResource;
import com.partidoya.platform.notifications.dto.NotificationResourceAssembler;
import com.partidoya.platform.notifications.services.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** Notificaciones del usuario autenticado; el destinatario siempre se toma del token. */
@RestController
@RequestMapping(value = "/api/v1/notifications", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Match notifications for the authenticated user")
public class NotificationsController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationsController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<NotificationPageResource> findMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = notificationService.findForRecipient(currentUserService.userId(), page, size);
        return ResponseEntity.ok(NotificationResourceAssembler.toPageResource(result));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResource> markAsRead(@PathVariable Long notificationId) {
        var notification = notificationService.markAsRead(notificationId, currentUserService.userId());
        return ResponseEntity.ok(NotificationResourceAssembler.toResource(notification));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(currentUserService.userId());
        return ResponseEntity.noContent().build();
    }

    /** El frontend lo llama al abrir el chat de un partido. */
    @PostMapping("/matches/{matchId}/chat/read")
    public ResponseEntity<Void> markChatAsRead(@PathVariable Long matchId) {
        notificationService.markChatAsRead(currentUserService.userId(), new MatchId(matchId));
        return ResponseEntity.noContent().build();
    }
}
