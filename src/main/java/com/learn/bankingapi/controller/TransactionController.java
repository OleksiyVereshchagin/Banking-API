package com.learn.bankingapi.controller;

import com.learn.bankingapi.controller.openapi.TransactionApi;
import com.learn.bankingapi.dto.filter.TransactionFilter;
import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.dto.response.transaction.PageResponse;
import com.learn.bankingapi.dto.response.transaction.TransactionResponse;
import com.learn.bankingapi.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @Override
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.transfer(request);
    }

    @GetMapping("/transactions")
    @Override
    public PageResponse<TransactionResponse> getTransactions(
            TransactionFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.getTransactions(filter, pageable);
    }

    @GetMapping("/accounts/{id}/transactions")
    @Override
    public PageResponse<TransactionResponse> getTransactionsByAccount(
            @PathVariable Long id,
            TransactionFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.getTransactionsByAccount(id, filter, pageable);
    }
}