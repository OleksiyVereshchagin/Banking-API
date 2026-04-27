package com.learn.bankingapi.controller;

import com.learn.bankingapi.dto.request.card.CreateCardRequest;
import com.learn.bankingapi.dto.request.card.UpdateCardStatusRequest;
import com.learn.bankingapi.dto.response.card.CardResponse;
import com.learn.bankingapi.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/accounts/{accountId}/cards")
    public CardResponse createCard(@PathVariable long accountId, @RequestBody CreateCardRequest request){
        return cardService.createCard(accountId, request);
    }

    @GetMapping("/accounts/{accountId}/cards")
    public List<CardResponse> getCardsByAccount(@PathVariable long accountId){
        return cardService.getCardsByAccount(accountId);
    }

    @GetMapping("/cards/{id}")
    public CardResponse getCardDetails(@PathVariable long id){
        return cardService.getCardDetails(id);
    }

    @PatchMapping("/cards/{id}/status")
    public CardResponse updateCardStatus(@RequestBody UpdateCardStatusRequest request, @PathVariable long id) {
        return cardService.updateCardStatus(request, id);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable long id){
        cardService.deleteCard(id);
    }
}
