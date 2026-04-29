package com.learn.bankingapi.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request")
public record LoginRequest(
        @Schema(description = "Login (email or phone)", example = "+380991234567")
        @NotBlank String login,

        @Schema(description = "Password", example = "Password123")
        @NotBlank String password
) {}
