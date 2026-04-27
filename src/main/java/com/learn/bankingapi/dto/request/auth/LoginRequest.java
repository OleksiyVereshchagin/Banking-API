package com.learn.bankingapi.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String login,
        @NotBlank String password
) {}
