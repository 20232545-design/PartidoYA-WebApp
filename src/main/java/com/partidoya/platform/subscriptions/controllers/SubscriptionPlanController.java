package com.partidoya.platform.subscriptions.controllers;

import com.partidoya.platform.subscriptions.dto.SubscriptionPlanResource;
import com.partidoya.platform.subscriptions.services.SubscriptionCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-plans")
public class SubscriptionPlanController {
    private final SubscriptionCatalogService catalogService;

    public SubscriptionPlanController(SubscriptionCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResource>> findActive() {
        return ResponseEntity.ok(catalogService.findActive());
    }
}
