package com.learn.bankingapi.dto.request.card;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.CardType;

@Schema(description = "Request to create a new bank card")
public record CreateCardRequest(
        @Schema(description = "Type of the card", example = "DEBIT")
        CardType type
) {}
