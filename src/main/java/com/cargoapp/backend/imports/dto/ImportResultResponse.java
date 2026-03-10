package com.cargoapp.backend.imports.dto;

import java.util.List;

public record ImportResultResponse(
        int imported,
        List<ImportErrorDto> errors
) {
}
