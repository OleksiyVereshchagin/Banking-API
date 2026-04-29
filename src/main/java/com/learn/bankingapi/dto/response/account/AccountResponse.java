package com.learn.bankingapi.dto.response.account;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Bank account details response")
public record AccountResponse(
        @Schema(description = "Account ID", example = "1")
        Long id,
        @Schema(description = "Current balance", example = "1500.50")
        BigDecimal balance,
        @Schema(description = "International Bank Account Number", example = "UA123456789012345678901234567")
        String iban,
        @Schema(description = "Account currency", example = "USD")
        Currency currency,
        @Schema(description = "Account status", example = "ACTIVE")
        AccountStatus status,
        @Schema(description = "Account creation date and time")
        LocalDateTime createdAt
) {}
