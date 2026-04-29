package com.learn.bankingapi.controller.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ExceptionResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "Error message", example = "Validation error")
        String message,
        @Schema(description = "Error timestamp")
        LocalDateTime timestamp) {
    public static ExceptionResponse error(int status, String message) {
        return new ExceptionResponse(status, message, LocalDateTime.now());
    }
}
