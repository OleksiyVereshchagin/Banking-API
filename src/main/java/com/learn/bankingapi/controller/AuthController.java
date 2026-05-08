package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.openapi.AuthApi;
import com.learn.bankingapi.dto.request.auth.RefreshTokenRequest;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.dto.request.auth.LoginRequest;
import com.learn.bankingapi.dto.request.auth.RegisterRequest;
import com.learn.bankingapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Override
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
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
}