package com.learn.bankingapi.dto.request.auth;

import com.learn.bankingapi.annotation.ValidLogin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @ValidLogin
        String login,
        @Size(min = 8, message = "Password must be at least 8 characters")
        @NotBlank
        String password,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        String middleName,
        @Past(message = "Date of birth must be in the past")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull
        LocalDate dateOfBirth) {}
