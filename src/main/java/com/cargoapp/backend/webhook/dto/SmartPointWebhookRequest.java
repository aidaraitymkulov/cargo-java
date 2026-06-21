package com.cargoapp.backend.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SmartPointWebhookRequest(
        String status,
        @JsonProperty("client_code") String clientCode,
        BigDecimal weight,
        BigDecimal price,
        String comment
) {}