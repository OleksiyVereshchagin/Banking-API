package com.learn.bankingapi.dto.response.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Transaction details response")
public record TransactionResponse(
        @Schema(description = "Transaction ID", example = "101")
        Long id,
        @Schema(description = "Transaction type", example = "TRANSFER")
        TransactionType type,
        @Schema(description = "Transaction status", example = "COMPLETED")
        TransactionStatus status,
        @Schema(description = "Transaction amount", example = "100.00")
        BigDecimal amount,
        @Schema(description = "Commission amount", example = "0.50")
        BigDecimal commission,
        @Schema(description = "Source account ID", example = "1")
        Long fromAccountId,
        @Schema(description = "Target account ID", example = "2")
        Long toAccountId,
        @Schema(description = "Transaction currency", example = "USD")
        Currency currency,
        @Schema(description = "Date and time of the transaction")
        LocalDateTime createdAt
) {}