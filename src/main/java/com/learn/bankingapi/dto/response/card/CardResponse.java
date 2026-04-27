package com.learn.bankingapi.dto.response.card;

import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.CardType;

import java.time.LocalDate;

public record CardResponse(
        Long id,
        String maskedCardNumber,
        LocalDate expirationDate,
        CardStatus status,
        CardType type,
        boolean isDefault
) {}
