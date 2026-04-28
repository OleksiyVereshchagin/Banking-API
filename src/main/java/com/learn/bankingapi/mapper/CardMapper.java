package com.learn.bankingapi.mapper;

import com.learn.bankingapi.entity.Card;
import com.learn.bankingapi.dto.response.card.CardResponse;
import com.learn.bankingapi.utils.CardMasker;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {
    private final CardMasker cardMasker;

    public CardMapper(CardMasker cardMasker) {
        this.cardMasker = cardMasker;
    }

    public CardResponse toDto(Card card){
        return new CardResponse(
                card.getId(),
                cardMasker.mask(card.getCardNumber()),
                card.getExpirationDate(),
                card.getStatus(),
                card.getType(),
                card.isDefault()
        );
    }
}
