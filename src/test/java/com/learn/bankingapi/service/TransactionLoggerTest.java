package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;
import com.learn.bankingapi.repository.TransactionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLoggerTest {

    @Mock
    private TransactionalRepository transactionalRepository;

    @InjectMocks
    private TransactionLogger transactionLogger;

    private Account source;
    private Account target;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        source = new Account();
        source.setId(1L);
        source.setUser(user);
        source.setCurrency(Currency.USD);

        target = new Account();
        target.setId(2L);
        target.setUser(user);
        target.setCurrency(Currency.USD);
    }

    @Test
    void createPending_TransferSuccess() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.TRANSFER, "key", 1L, 2L);
        
        when(transactionalRepository.saveAndFlush(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionLogger.createPending(source, target, request, "key");

        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals(TransactionType.TRANSFER, result.getType());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void markAsCompleted_Success() {
        transactionLogger.markAsCompleted(10L);
        verify(transactionalRepository).updateStatus(10L, TransactionStatus.COMPLETED, null);
    }

    @Test
    void markAsFailed_Success() {
        transactionLogger.markAsFailed(10L, "Error");
        verify(transactionalRepository).updateStatus(10L, TransactionStatus.FAILED, "Error");
    }
}
