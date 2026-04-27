package com.learn.bankingapi.dto.request.pin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String newPin,

        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String oldPin
) {}
