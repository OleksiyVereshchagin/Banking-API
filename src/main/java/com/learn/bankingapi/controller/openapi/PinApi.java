package com.learn.bankingapi.controller.openapi;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.pin.ChangePinRequest;
import com.learn.bankingapi.dto.request.pin.SetPinRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "PIN Controller", description = "Management of card PIN codes")
public interface PinApi {

    @Operation(summary = "Set card PIN", description = "Sets the initial PIN for a card")
    @ApiResponse(responseCode = "204", description = "PIN successfully set")
    @ApiResponse(responseCode = "400", description = "Invalid request or PIN mismatch",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"PINs do not match\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    void setPin(@Valid @RequestBody SetPinRequest request, @PathVariable long id);

    @Operation(summary = "Change card PIN", description = "Updates an existing card PIN")
    @ApiResponse(responseCode = "204", description = "PIN successfully changed")
    @ApiResponse(responseCode = "400", description = "Invalid old PIN or new PIN same as old",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Invalid old PIN\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Card not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    void changePin(@Valid @RequestBody ChangePinRequest request, @PathVariable long id);
}