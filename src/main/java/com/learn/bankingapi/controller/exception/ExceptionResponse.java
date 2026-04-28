package com.learn.bankingapi.controller.exception;

import java.time.LocalDateTime;

public record ExceptionResponse(int status, String message, LocalDateTime timestamp) {
    public static ExceptionResponse error(int status, String message) {
        return new ExceptionResponse(status, message, LocalDateTime.now());
    }
}
