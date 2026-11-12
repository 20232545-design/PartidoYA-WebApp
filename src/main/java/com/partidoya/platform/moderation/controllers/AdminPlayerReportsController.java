package com.partidoya.platform.moderation.controllers;

import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.moderation.dto.PlayerReportPageResource;
import com.partidoya.platform.moderation.dto.PlayerReportResource;
import com.partidoya.platform.moderation.dto.PlayerReportResourceAssembler;
import com.partidoya.platform.moderation.dto.ResolvePlayerReportResource;
import com.partidoya.platform.moderation.models.PlayerReportStatus;
import com.partidoya.platform.moderation.services.PlayerReportService;
import com.partidoya.platform.users.models.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** Moderación de reportes: solo para administradores generales de la plataforma. */
@RestController
@RequestMapping(value = "/api/v1/admin/player-reports", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Player Reports (admin)", description = "Review and resolve player reports")
public class AdminPlayerReportsController {
    private final PlayerReportService reportService;
    private final CurrentUserService currentUserService;

    public AdminPlayerReportsController(PlayerReportService reportService, CurrentUserService currentUserService) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
    }

    /** {@code status} vacío o ALL devuelve reportes de todos los estados. */
    @GetMapping
    public ResponseEntity<PlayerReportPageResource> search(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireRole(Role.ADMIN_GENERAL);
        var result = reportService.search(parseStatus(status), page, size);
        return ResponseEntity.ok(PlayerReportResourceAssembler.toPageResource(result));
    }

    @PatchMapping(value = "/{reportId}/resolve", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<PlayerReportResource> resolve(@PathVariable Long reportId,
                                                        @RequestBody ResolvePlayerReportResource resource) {
        var adminId = currentUserService.requireRole(Role.ADMIN_GENERAL);
        var view = reportService.resolve(reportId, adminId, resource.outcome(), resource.note());
        return ResponseEntity.ok(PlayerReportResourceAssembler.toResource(view));
    }

    private static PlayerReportStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return null;
        try {
            return PlayerReportStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown report status: " + status);
        }
    }
}
