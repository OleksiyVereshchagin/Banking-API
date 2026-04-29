package com.learn.bankingapi.dto.request.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.TransactionType;

import java.math.BigDecimal;

@Schema(description = "Request to create a new transaction (Transfer, Deposit, or Withdrawal)")
public record CreateTransactionRequest(
        @Schema(description = "Amount of the transaction", example = "100.00")
        BigDecimal amount,
        @Schema(description = "Type of the transaction", example = "TRANSFER")
        TransactionType type,
        @Schema(description = "Unique key to prevent duplicate processing", example = "550e8400-e29b-41d4-a716-446655440000")
        String idempotencyKey,
        @Schema(description = "Source account ID (required for TRANSFER and WITHDRAWAL)", example = "1")
        Long fromAccountId,
        @Schema(description = "Target account ID (required for TRANSFER and DEPOSIT)", example = "2")
        Long toAccountId
) {}
