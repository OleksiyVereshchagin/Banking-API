package com.learn.bankingapi.dto.response.auth;

public record AuthLoginResponse(String token, String refreshToken) {
}
