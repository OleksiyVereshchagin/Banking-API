package com.learn.bankingapi.dto.response.user;

import com.learn.bankingapi.enums.UserRole;

import java.time.LocalDate;

public record UserResponse(
        String phoneNumber,
        String email,
        String firstName,
        String lastName,
        String middleName,
        LocalDate dateOfBirth,
        UserRole role
) {}
