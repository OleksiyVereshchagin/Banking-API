package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.openapi.AccountApi;
import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import com.learn.bankingapi.service.AccountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController implements AccountApi {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Override
    public AccountResponse createAccount(@RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping
    @Override
    public AccountContainerResponse getAccounts() {
        return accountService.getAccounts();
    }

    @GetMapping("/{id}")
    @Override
    public AccountResponse getAccountDetails(@PathVariable long id) {
        return accountService.getAccountDetails(id);
    }

    @PatchMapping("/{id}/status")
    @Override
    public AccountEditStatusResponse updateAccountStatus(@PathVariable long id, @RequestBody UpdateAccountStatusRequest request) {
        return accountService.updateAccountStatus(id, request);
    }
}