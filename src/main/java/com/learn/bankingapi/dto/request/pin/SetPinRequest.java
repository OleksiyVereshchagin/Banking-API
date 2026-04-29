package com.learn.bankingapi.dto.request.pin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to set a new PIN for a card")
public record SetPinRequest(
        @Schema(description = "4-digit PIN", example = "1234")
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String pin,

        @Schema(description = "Confirm 4-digit PIN", example = "1234")
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String confirmPin
) {}
