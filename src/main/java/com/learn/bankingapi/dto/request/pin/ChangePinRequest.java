package com.learn.bankingapi.dto.request.pin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to change an existing card PIN")
public record ChangePinRequest(
        @Schema(description = "New 4-digit PIN", example = "4321")
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String newPin,

        @Schema(description = "Current 4-digit PIN", example = "1234")
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String oldPin
) {}
