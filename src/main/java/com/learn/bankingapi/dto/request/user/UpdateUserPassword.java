package com.learn.bankingapi.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserPassword(
        @NotBlank
        @Size(min = 8)
        String currentPassword,

        @NotBlank
        @Size(min = 8)
        String newPassword
) {}
