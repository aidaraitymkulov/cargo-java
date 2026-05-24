package com.cargoapp.backend.dashboard.dto;

import java.time.LocalDate;

public record DailyStatResponse(
        LocalDate date,
        long count
) {}
