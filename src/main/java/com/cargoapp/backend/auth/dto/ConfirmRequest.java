package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(
        @NotBlank String login,
        @NotBlank String code
) {
}