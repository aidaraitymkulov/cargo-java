package com.cargoapp.backend.imports.dto;

public record ImportErrorDto(
        int row,
        String reason,
        String detail
) {
}
