package com.learn.bankingapi.mapper;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AccountMapper {
    public AccountResponse toDto(Account account){
        return new AccountResponse(
                account.getId(),
                account.getBalance(),
                account.getIban(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public AccountEditStatusResponse toStatusUpdateDto(Account account, LocalDateTime editTime){
        return new AccountEditStatusResponse(
                account.getId(),
                account.getIban(),
                account.getCurrency(),
                account.getStatus(),
                editTime
        );
    }
}
