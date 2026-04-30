package com.learn.bankingapi.utils;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AccountAccessValidatorTest {

    private AccountAccessValidator validator;
    private Account account;

    @BeforeEach
    void setUp() {
        validator = new AccountAccessValidator();
        account = new Account();
    }

    @Test
    void validateCanOperate_ActiveAccount_Success() {
        account.setStatus(AccountStatus.ACTIVE);
        assertDoesNotThrow(() -> validator.validateCanOperate(account));
    }

    @Test
    void validateCanOperate_ClosedAccount_ThrowsException() {
        account.setStatus(AccountStatus.CLOSED);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> validator.validateCanOperate(account));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Account is not active", exception.getReason());
    }

    @Test
    void validateCanOperate_BlockedAccount_ThrowsException() {
        account.setStatus(AccountStatus.BLOCKED);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> validator.validateCanOperate(account));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Account is not active", exception.getReason());
    }
}
