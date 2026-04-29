package com.learn.bankingapi.dto.response.user;

import com.learn.bankingapi.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "User profile data")
public record UserResponse(
        @Schema(description = "Phone number", example = "+380991234567")
        String phoneNumber,
        @Schema(description = "Email address", example = "user@example.com")
        String email,
        @Schema(description = "First name", example = "John")
        String firstName,
        @Schema(description = "Last name", example = "Doe")
        String lastName,
        @Schema(description = "Middle name", example = "Smith")
        String middleName,
        @Schema(description = "Date of birth", example = "1990-01-01")
        LocalDate dateOfBirth,
        @Schema(description = "User role", example = "USER")
        UserRole role
) {}
