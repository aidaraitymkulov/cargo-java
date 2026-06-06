package com.cargoapp.backend.auth.event;

public record PasswordResetEmailEvent(String email, String code) {}
