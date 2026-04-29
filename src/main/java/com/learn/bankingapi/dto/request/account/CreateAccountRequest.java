package com.learn.bankingapi.dto.request.account;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.Currency;

@Schema(description = "Request to create a new bank account")
public record CreateAccountRequest(
        @Schema(description = "Account currency", example = "USD")
        Currency currency
) {}
