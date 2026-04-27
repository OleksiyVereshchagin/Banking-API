package com.learn.bankingapi.dto.request.transaction;

import com.learn.bankingapi.enums.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        BigDecimal amount,
        TransactionType type,
        String idempotencyKey,
        Long fromAccountId,
        Long toAccountId
) {}
