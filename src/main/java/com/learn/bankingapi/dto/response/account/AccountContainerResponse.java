package com.learn.bankingapi.dto.response.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Container for a list of user accounts")
public record AccountContainerResponse(
        @Schema(description = "List of bank accounts")
        List<AccountResponse> accounts
) {}
