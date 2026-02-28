package com.cargoapp.backend.users.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeBranchRequest(@NotNull UUID branchId) {}
