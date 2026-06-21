package com.cargoapp.backend.webhook.controller;

import com.cargoapp.backend.smartpoint.SmartPointProperties;
import com.cargoapp.backend.webhook.dto.SmartPointWebhookRequest;
import com.cargoapp.backend.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final SmartPointProperties properties;

    @PostMapping("/smartpoint/status/{trackNumber}")
    public ResponseEntity<Void> handleStatus(
            @PathVariable String trackNumber,
            @RequestHeader(value = "x-ades-token", required = false) String token,
            @RequestBody SmartPointWebhookRequest request
    ) {
        String secret = properties.webhookSecret();
        if (secret == null || !secret.equals(token)) {
            log.warn("Webhook: unauthorized request for track={}", trackNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Webhook: received status={} track={} clientCode={}", request.status(), trackNumber, request.clientCode());
        webhookService.handleStatus(trackNumber, request.status(), request.clientCode(), request.weight(), request.price());
        return ResponseEntity.ok().build();
    }
}