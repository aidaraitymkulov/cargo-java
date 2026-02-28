package com.cargoapp.backend.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email String email,
        @Size(max = 20) String phone
) {}
