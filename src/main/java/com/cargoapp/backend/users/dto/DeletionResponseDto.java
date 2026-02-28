package com.cargoapp.backend.users.dto;

import java.time.LocalDateTime;

public record DeletionResponseDto(boolean success, LocalDateTime deletionDate) {}
