package com.learn.bankingapi.dto.response.card;

import java.util.List;

public record CardsContainerResponse(
        List<CardResponse> cards,
        int totalCards
) {}
