package com.learn.bankingapi.utils;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.enums.AccountStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountAccessValidator {
    public void validateCanOperate(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not active");
        }
    }
}
