package com.learn.bankingapi.mapper;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.dto.response.transaction.PageResponse;
import com.learn.bankingapi.dto.response.transaction.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {

    public TransactionResponse toDto(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getCommission(),
                getAccountId(transaction.getFromAccount()),
                getAccountId(transaction.getToAccount()),
                transaction.getCurrency(),
                transaction.getCreatedAt()
        );
    }

    private Long getAccountId(Account account) {
        return account != null ? account.getId() : null;
    }

    public PageResponse<TransactionResponse> toPageResponse(Page<Transaction> page) {
        List<TransactionResponse> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}