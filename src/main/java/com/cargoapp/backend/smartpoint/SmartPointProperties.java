package com.cargoapp.backend.smartpoint;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.smartpoint")
public record SmartPointProperties(
        String apiUrl,
        String apiToken,
        String webhookSecret
) {}