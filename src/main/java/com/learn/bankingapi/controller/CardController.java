package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.card.CreateCardRequest;
import com.learn.bankingapi.dto.request.card.UpdateCardStatusRequest;
import com.learn.bankingapi.dto.response.card.CardResponse;
import com.learn.bankingapi.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Card Controller", description = "Management of bank cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/accounts/{accountId}/cards")
    @Operation(summary = "Create a new card", description = "Creates a new card for the specified account")
    @ApiResponse(responseCode = "200", description = "Card successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request or account limit reached", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Account has reached the maximum number of cards\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "404", description = "Account not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public CardResponse createCard(@PathVariable long accountId, @RequestBody CreateCardRequest request){
        return cardService.createCard(accountId, request);
    }

    @GetMapping("/accounts/{accountId}/cards")
    @Operation(summary = "Get cards by account", description = "Returns a list of all cards associated with the specified account")
    @ApiResponse(responseCode = "200", description = "List of cards successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public List<CardResponse> getCardsByAccount(@PathVariable long accountId){
        return cardService.getCardsByAccount(accountId);
    }

    @GetMapping("/cards/{id}")
    @Operation(summary = "Get card details", description = "Returns detailed information about a specific card")
    @ApiResponse(responseCode = "200", description = "Card details successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Card not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public CardResponse getCardDetails(@PathVariable long id){
        return cardService.getCardDetails(id);
    }

    @PatchMapping("/cards/{id}/status")
    @Operation(summary = "Update card status", description = "Changes the status of a specific card (e.g., ACTIVE, BLOCKED)")
    @ApiResponse(responseCode = "200", description = "Card status successfully updated")
    @ApiResponse(responseCode = "404", description = "Card not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public CardResponse updateCardStatus(@RequestBody UpdateCardStatusRequest request, @PathVariable long id) {
        return cardService.updateCardStatus(request, id);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete card", description = "Permanently deletes a specific card")
    @ApiResponse(responseCode = "204", description = "Card successfully deleted")
    @ApiResponse(responseCode = "404", description = "Card not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public void deleteCard(@PathVariable long id){
        cardService.deleteCard(id);
    }
}
