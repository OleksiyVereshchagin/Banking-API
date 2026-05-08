package com.learn.bankingapi.controller.openapi;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Account Controller", description = "Management of bank accounts")
public interface AccountApi {

    @Operation(summary = "Create a new account", description = "Creates a new bank account with the specified currency for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Account successfully created")
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"User is not authorized\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    AccountResponse createAccount(@RequestBody CreateAccountRequest request);

    @Operation(summary = "Get all accounts", description = "Returns a list of all accounts belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of accounts successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"User is not authorized\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    AccountContainerResponse getAccounts();

    @Operation(summary = "Get account details", description = "Returns detailed information about a specific account")
    @ApiResponse(responseCode = "200", description = "Account details successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    AccountResponse getAccountDetails(@PathVariable long id);

    @Operation(summary = "Update account status", description = "Changes the status of a specific account (e.g., ACTIVE, CLOSED)")
    @ApiResponse(responseCode = "200", description = "Account status successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid status or business rule violation",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 400, \"message\": \"Cannot close account with non-zero balance\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    AccountEditStatusResponse updateAccountStatus(@PathVariable long id, @RequestBody UpdateAccountStatusRequest request);
}