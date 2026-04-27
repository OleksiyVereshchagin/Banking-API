package com.learn.bankingapi.controller;

import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import com.learn.bankingapi.service.AccountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public AccountResponse createAccount(@RequestBody CreateAccountRequest request){
        return accountService.createAccount(request);
    }

    @GetMapping
    public AccountContainerResponse getAccounts(){
        return accountService.getAccounts();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountDetails(@PathVariable long id){
        return accountService.getAccountDetails(id);
    }

    @PatchMapping("/{id}/status")
    public AccountEditStatusResponse updateAccountStatus(@PathVariable long id, @RequestBody UpdateAccountStatusRequest request){
        return accountService.updateAccountStatus(id, request);
    }
}
