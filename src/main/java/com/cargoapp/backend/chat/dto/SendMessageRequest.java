package com.cargoapp.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID roomId,
        @NotBlank @Size(max = 2000) String content
) {}