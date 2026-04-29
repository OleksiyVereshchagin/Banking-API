package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.auth.RefreshTokenRequest;
import com.learn.bankingapi.dto.response.auth.AuthLoginResponse;
import com.learn.bankingapi.dto.response.auth.AuthResponse;
import com.learn.bankingapi.dto.request.auth.LoginRequest;
import com.learn.bankingapi.dto.request.auth.RegisterRequest;
import com.learn.bankingapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Controller", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user profile with the provided details")
    @ApiResponse(responseCode = "200", description = "User successfully registered")
    @ApiResponse(responseCode = "400", description = "Invalid input data", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"User with this login already exists\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "422", description = "Validation error", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 422, \"message\": \"password: Password must be at least 8 characters\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AuthResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.registration(request);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user and returns access and refresh tokens")
    @ApiResponse(responseCode = "200", description = "Successfully authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"Invalid credentials\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AuthLoginResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Provides a new access token using a valid refresh token")
    @ApiResponse(responseCode = "200", description = "Token successfully refreshed")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"Invalid token\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AuthResponse refreshToken(@RequestBody RefreshTokenRequest token){
        return authService.refreshToken(token.refreshToken());
    }
}
