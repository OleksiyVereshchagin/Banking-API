package com.learn.bankingapi.dto.response.pin;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.CardStatus;

@Schema(description = "Response after PIN operation")
public record PinResponse(
        @Schema(description = "Card ID", example = "1")
        Long cardId,
        @Schema(description = "Current card status", example = "ACTIVE")
        CardStatus status
) {}
