package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.openapi.AuthApi;
import com.learn.bankingapi.dto.request.auth.*;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.dto.response.auth.RegistrationResponse;
import com.learn.bankingapi.service.AuthService;
import com.learn.bankingapi.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final VerificationService verificationService;

    public AuthController(AuthService authService, VerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    @Override
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.registration(request);
    }

    @PostMapping("/login")
    @Override
    public AuthLoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    @Override
    public AuthResponse refreshToken(@RequestBody RefreshTokenRequest token) {
        return authService.refreshToken(token.refreshToken());
    }

    @PostMapping("/verify")
    public AuthResponse verify(@RequestBody VerifyRequest request) {
        return verificationService.verify(request);
    }

    @PostMapping("/resend-verefication")
    public void resendVerefication(@RequestBody ResendRequest request) {
        verificationService.resend(request);
    }
}