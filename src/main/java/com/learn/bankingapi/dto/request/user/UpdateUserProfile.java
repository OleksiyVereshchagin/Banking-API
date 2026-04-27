package com.learn.bankingapi.dto.request.user;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserProfile(
        @Size(max = 40)
        String firstName,
        @Size(max = 40)
        String lastName,
        @Size(max = 40)
        String middleName,
        @Past
        LocalDate dateOfBirth
) { }
