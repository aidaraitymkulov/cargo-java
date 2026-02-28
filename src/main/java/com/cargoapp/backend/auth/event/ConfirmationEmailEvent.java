package com.cargoapp.backend.auth.event;

public record ConfirmationEmailEvent(String email, String code) {
}
