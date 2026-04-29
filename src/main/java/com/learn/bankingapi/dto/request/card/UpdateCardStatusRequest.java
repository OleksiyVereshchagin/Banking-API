package com.learn.bankingapi.dto.request.card;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.CardStatus;

@Schema(description = "Request to update card status")
public record UpdateCardStatusRequest(
        @Schema(description = "New card status", example = "BLOCKED")
        CardStatus status
) {}
