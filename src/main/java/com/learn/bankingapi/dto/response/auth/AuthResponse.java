package com.learn.bankingapi.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic authentication response with JWT")
public record AuthResponse(
        @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token
) {
}
