package com.learn.bankingapi.dto.response.card;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Container for a list of bank cards")
public record CardsContainerResponse(
        @Schema(description = "List of cards")
        List<CardResponse> cards,
        @Schema(description = "Total number of cards", example = "2")
        int totalCards
) {}
