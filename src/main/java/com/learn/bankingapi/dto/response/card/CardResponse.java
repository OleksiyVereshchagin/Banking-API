package com.learn.bankingapi.dto.response.card;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.CardType;

import java.time.LocalDate;

@Schema(description = "Bank card details response")
public record CardResponse(
        @Schema(description = "Card ID", example = "1")
        Long id,
        @Schema(description = "Masked card number", example = "4444 44** **** 4444")
        String maskedCardNumber,
        @Schema(description = "Expiration date", example = "2028-12-31")
        LocalDate expirationDate,
        @Schema(description = "Card status", example = "ACTIVE")
        CardStatus status,
        @Schema(description = "Card type", example = "DEBIT")
        CardType type,
        @Schema(description = "Is this the default card for the account", example = "true")
        boolean isDefault
) {}
