package com.learn.bankingapi.dto.request.account;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.bankingapi.enums.AccountStatus;

@Schema(description = "Request to update account status")
public record UpdateAccountStatusRequest(
        @Schema(description = "New account status", example = "ACTIVE")
        AccountStatus status
) {}
