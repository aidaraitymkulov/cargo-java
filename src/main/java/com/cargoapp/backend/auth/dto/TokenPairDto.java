package com.cargoapp.backend.auth.dto;

public record TokenPairDto(
        String accessToken,
        String refreshToken
) {}

