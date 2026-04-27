package com.learn.bankingapi.dto.response.account;

import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        BigDecimal balance,
        String iban,
        Currency currency,
        AccountStatus status,
        LocalDateTime createdAt
) {}
