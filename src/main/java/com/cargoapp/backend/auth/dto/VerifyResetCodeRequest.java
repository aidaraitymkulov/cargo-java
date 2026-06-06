package com.cargoapp.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyResetCodeRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "Код должен состоять из 4 цифр") String code
) {}
