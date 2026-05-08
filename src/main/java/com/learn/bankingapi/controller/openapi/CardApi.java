package com.learn.bankingapi.controller.openapi;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.card.CreateCardRequest;
import com.learn.bankingapi.dto.request.card.UpdateCardStatusRequest;
import com.learn.bankingapi.dto.response.card.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Card Controller", description = "Management of bank cards")
public interface CardApi {

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
    CardResponse createCard(@PathVariable long accountId, @RequestBody CreateCardRequest request);

    @Operation(summary = "Get cards by account", description = "Returns a list of all cards associated with the specified account")
    @ApiResponse(responseCode = "200", description = "List of cards successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    List<CardResponse> getCardsByAccount(@PathVariable long accountId);

    @Operation(summary = "Get card details", description = "Returns detailed information about a specific card")
    @ApiResponse(responseCode = "200", description = "Card details successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    CardResponse getCardDetails(@PathVariable long id);

    @Operation(summary = "Update card status", description = "Changes the status of a specific card (e.g., ACTIVE, BLOCKED)")
    @ApiResponse(responseCode = "200", description = "Card status successfully updated")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    CardResponse updateCardStatus(@RequestBody UpdateCardStatusRequest request, @PathVariable long id);

    @Operation(summary = "Delete card", description = "Permanently deletes a specific card")
    @ApiResponse(responseCode = "204", description = "Card successfully deleted")
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    void deleteCard(@PathVariable long id);
}