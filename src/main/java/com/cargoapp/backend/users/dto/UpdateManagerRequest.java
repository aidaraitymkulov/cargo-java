package com.cargoapp.backend.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateManagerRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20) String phone
) {}
