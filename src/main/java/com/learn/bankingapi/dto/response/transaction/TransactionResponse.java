package com.learn.bankingapi.dto.response.transaction;

import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal commission,
        Long fromAccountId,
        Long toAccountId,
        Currency currency,
        LocalDateTime createdAt
) {}