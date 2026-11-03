package com.partidoya.platform.subscriptions.controllers;

import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.subscriptions.dto.CreateSubscriptionResource;
import com.partidoya.platform.subscriptions.dto.CulqiWebhookResource;
import com.partidoya.platform.subscriptions.dto.MySubscriptionResource;
import com.partidoya.platform.subscriptions.dto.SubscriptionResource;
import com.partidoya.platform.subscriptions.services.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    public SubscriptionController(SubscriptionService subscriptionService, CurrentUserService currentUserService) {
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResource> create(@RequestBody CreateSubscriptionResource resource) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(currentUserService.userId(), resource));
    }

    @GetMapping("/me")
    public ResponseEntity<MySubscriptionResource> findMine() {
        return ResponseEntity.ok(subscriptionService.findMine(currentUserService.userId()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<SubscriptionResource> cancelMine() {
        return ResponseEntity.ok(subscriptionService.cancelMine(currentUserService.userId()));
    }

    @PostMapping("/webhooks/culqi")
    public ResponseEntity<Void> processCulqi(@RequestBody CulqiWebhookResource resource) {
        subscriptionService.processCulqiEvent(resource.id());
        return ResponseEntity.ok().build();
    }
}
