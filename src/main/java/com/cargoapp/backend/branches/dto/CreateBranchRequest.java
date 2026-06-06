package com.cargoapp.backend.branches.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(

        @NotBlank(message = "Адрес обязателен")
        String address,

        @NotBlank(message = "Префикс обязателен")
        String personalCodePrefix,

        Double latitude,
        Double longitude,
        String phone,
        String workingHours
) {
}
