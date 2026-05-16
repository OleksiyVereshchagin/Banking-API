package com.learn.bankingapi.dto.request.auth;

import com.learn.bankingapi.enums.VerificationType;

public record VerifyRequest(
        String login,
        String token,
        VerificationType type
) {
}
