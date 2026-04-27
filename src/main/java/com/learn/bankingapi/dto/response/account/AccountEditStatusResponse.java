package com.learn.bankingapi.dto.response.account;

import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;

import java.time.LocalDateTime;

public record AccountEditStatusResponse(
        long id,
        String iban,
        Currency currency,
        AccountStatus status,
        LocalDateTime editTime
) {}
