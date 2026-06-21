package com.cargoapp.backend.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkAsReadRequest(
        @NotNull UUID roomId
) {}