package com.learn.bankingapi.dto.response.account;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;

import java.time.LocalDateTime;

@Schema(description = "Response after updating account status")
public record AccountEditStatusResponse(
        @Schema(description = "Account ID", example = "1")
        long id,
        @Schema(description = "International Bank Account Number", example = "UA123456789012345678901234567")
        String iban,
        @Schema(description = "Account currency", example = "USD")
        Currency currency,
        @Schema(description = "Updated account status", example = "CLOSED")
        AccountStatus status,
        @Schema(description = "Date and time of the status update")
        LocalDateTime editTime
) {}
