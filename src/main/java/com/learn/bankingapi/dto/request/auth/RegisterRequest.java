package com.learn.bankingapi.dto.request.auth;

import com.learn.bankingapi.annotation.ValidLogin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "User registration request")
public record RegisterRequest(
        @Schema(description = "Login (email or phone)", example = "user@example.com")
        @ValidLogin
        String login,

        @Schema(description = "Password", example = "Password123")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @NotBlank
        String password,

        @Schema(description = "First name", example = "John")
        @NotBlank
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        @NotBlank
        String lastName,

        @Schema(description = "Middle name", example = "Smith")
        String middleName,

        @Schema(description = "Date of birth", example = "1990-01-01")
        @Past(message = "Date of birth must be in the past")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull
        LocalDate dateOfBirth) {}
