package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResetPasswordRequest(
        @NotNull UUID resetToken,
        @NotBlank @Size(min = 6, max = 72) String newPassword
) {}
