package com.learn.bankingapi.dto.response.pin;

import com.learn.bankingapi.enums.CardStatus;

public record PinResponse(
        Long cardId,
        CardStatus status
) {}
