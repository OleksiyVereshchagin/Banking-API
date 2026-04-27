package com.learn.bankingapi.dto.request.account;

import com.learn.bankingapi.enums.AccountStatus;

public record UpdateAccountStatusRequest(
        AccountStatus status
) {}
