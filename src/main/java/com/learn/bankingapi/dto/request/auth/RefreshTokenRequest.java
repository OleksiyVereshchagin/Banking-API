package com.learn.bankingapi.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh token request")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token", example = "550e8400-e29b-41d4-a716-446655440000")
        String refreshToken
) {
}
