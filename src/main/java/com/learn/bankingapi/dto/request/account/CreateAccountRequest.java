package com.learn.bankingapi.dto.request.account;

import com.learn.bankingapi.enums.Currency;

public record CreateAccountRequest(
        Currency currency
) {}
