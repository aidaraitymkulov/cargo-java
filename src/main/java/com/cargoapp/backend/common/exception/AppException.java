package com.cargoapp.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public AppException(String errorCode, HttpStatus status) {
        super(errorCode);
        this.errorCode = errorCode;
        this.status = status;
    }

    public AppException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
