package com.learn.bankingapi.controller.openapi;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.filter.TransactionFilter;
import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.dto.response.transaction.PageResponse;
import com.learn.bankingapi.dto.response.transaction.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Transaction Controller", description = "Endpoints for money transfers, deposits, and withdrawals")
public interface TransactionApi {

    @Operation(summary = "Execute a transaction", description = "Processes a money transfer, deposit, or withdrawal")
    @ApiResponse(responseCode = "200", description = "Transaction successfully executed")
    @ApiResponse(responseCode = "400", description = "Business rule violation or invalid input",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Insufficient funds\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Source account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "422", description = "Validation error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 422, \"message\": \"amount: must be greater than 0\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request);

    @Operation(summary = "Get transaction history", description = "Returns a paginated list of transactions filtered by various criteria")
    @ApiResponse(responseCode = "200", description = "History successfully retrieved")
    PageResponse<TransactionResponse> getTransactions(TransactionFilter filter, Pageable pageable);

    @Operation(summary = "Get transactions by account", description = "Returns a paginated list of transactions for a specific account")
    @ApiResponse(responseCode = "200", description = "Account history successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    PageResponse<TransactionResponse> getTransactionsByAccount(@PathVariable Long id, TransactionFilter filter, Pageable pageable);
}