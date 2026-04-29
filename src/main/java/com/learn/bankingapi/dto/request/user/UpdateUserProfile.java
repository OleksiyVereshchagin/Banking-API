package com.learn.bankingapi.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Request to update user profile")
public record UpdateUserProfile(
        @Schema(description = "First name", example = "John")
        @Size(max = 40)
        String firstName,
        @Schema(description = "Last name", example = "Doe")
        @Size(max = 40)
        String lastName,
        @Schema(description = "Middle name", example = "Smith")
        @Size(max = 40)
        String middleName,
        @Schema(description = "Date of birth", example = "1990-01-01")
        @Past
        LocalDate dateOfBirth
) { }
