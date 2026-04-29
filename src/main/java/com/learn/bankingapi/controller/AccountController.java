package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.exception.ExceptionResponse;
import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import com.learn.bankingapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Controller", description = "Management of bank accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a new bank account with the specified currency for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Account successfully created")
    @ApiResponse(responseCode = "401", description = "Unauthorized", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"User is not authorized\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AccountResponse createAccount(@RequestBody CreateAccountRequest request){
        return accountService.createAccount(request);
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Returns a list of all accounts belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of accounts successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 401, \"message\": \"User is not authorized\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AccountContainerResponse getAccounts(){
        return accountService.getAccounts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details", description = "Returns detailed information about a specific account")
    @ApiResponse(responseCode = "200", description = "Account details successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found", 
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceptionResponse.class),
                    examples = @ExampleObject(value = "{\"status\": 404, \"message\": \"Account not found\", \"timestamp\": \"2024-05-20T10:00:00\"}")))
    public AccountResponse getAccountDetails(@PathVariable long id){
        return accountService.getAccountDetails(id);
    }

    @PatchMapping("/{id}/status")
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
    public AccountEditStatusResponse updateAccountStatus(@PathVariable long id, @RequestBody UpdateAccountStatusRequest request){
        return accountService.updateAccountStatus(id, request);
    }
}
