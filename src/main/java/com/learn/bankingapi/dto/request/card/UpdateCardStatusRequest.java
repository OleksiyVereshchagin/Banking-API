package com.learn.bankingapi.dto.request.card;

import com.learn.bankingapi.enums.CardStatus;

public record UpdateCardStatusRequest(CardStatus status) {}
