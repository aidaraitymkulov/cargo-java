package com.cargoapp.backend.orders.dto;

import java.time.LocalDate;

public record DailyCountResponse(LocalDate date, long count) {}
