package com.cargoapp.backend.common.constants;

public enum ClientType {
    MANAGER("manager"),
    USER("user");

    private final String value;

    ClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
