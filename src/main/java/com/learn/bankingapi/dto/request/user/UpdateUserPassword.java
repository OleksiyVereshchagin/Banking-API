package com.learn.bankingapi.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change password")
public record UpdateUserPassword(
        @Schema(description = "Current password", example = "OldPassword123")
        @NotBlank
        @Size(min = 8)
        String currentPassword,

        @Schema(description = "New password", example = "NewPassword123")
        @NotBlank
        @Size(min = 8)
        String newPassword
) {}
