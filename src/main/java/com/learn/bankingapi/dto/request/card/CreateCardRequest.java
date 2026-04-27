package com.learn.bankingapi.dto.request.card;

import com.learn.bankingapi.enums.CardType;

public record CreateCardRequest(
        CardType type
) {}
