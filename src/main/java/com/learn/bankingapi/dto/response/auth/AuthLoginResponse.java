package com.learn.bankingapi.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with tokens")
public record AuthLoginResponse(
        @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "Refresh Token", example = "550e8400-e29b-41d4-a716-446655440000")
        String refreshToken
) {
}
