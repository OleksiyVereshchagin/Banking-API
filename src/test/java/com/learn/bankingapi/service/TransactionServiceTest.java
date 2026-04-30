package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.dto.response.transaction.TransactionResponse;
import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.TransactionMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.repository.TransactionalRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogger transactionLogger;

    @Mock
    private TransactionalRepository transactionalRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(UserRole.USER);

        sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setUser(testUser);
        sourceAccount.setBalance(new BigDecimal("1000.00"));
        sourceAccount.setStatus(AccountStatus.ACTIVE);
        sourceAccount.setCurrency(Currency.USD);

        targetAccount = new Account();
        targetAccount.setId(2L);
        targetAccount.setUser(testUser);
        targetAccount.setBalance(new BigDecimal("500.00"));
        targetAccount.setStatus(AccountStatus.ACTIVE);
        targetAccount.setCurrency(Currency.USD);
    }

    @Test
    void transfer_Success() {
        String idempotencyKey = "key123";
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.TRANSFER, idempotencyKey, 1L, 2L);

        Transaction pendingTx = new Transaction();
        pendingTx.setId(10L);
        pendingTx.setStatus(TransactionStatus.PENDING);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(transactionalRepository.findByUserIdAndIdempotencyKey(1L, idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndStatusForUpdate(1L, AccountStatus.ACTIVE)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdAndStatusForUpdate(2L, AccountStatus.ACTIVE)).thenReturn(Optional.of(targetAccount));
        when(transactionLogger.createPending(eq(sourceAccount), eq(targetAccount), eq(request), eq(idempotencyKey)))
                .thenReturn(pendingTx);
        
        TransactionResponse txResponse = new TransactionResponse(10L, TransactionType.TRANSFER, TransactionStatus.COMPLETED, 
                new BigDecimal("100.00"), BigDecimal.ZERO, 1L, 2L, Currency.USD, LocalDateTime.now());
        when(transactionMapper.toDto(any())).thenReturn(txResponse);
        when(transactionalRepository.findById(10L)).thenReturn(Optional.of(pendingTx));

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertEquals(new BigDecimal("900.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("600.00"), targetAccount.getBalance());
        
        verify(transactionLogger).markAsCompleted(10L);
    }

    @Test
    void transfer_Idempotent_ReturnExisting() {
        String idempotencyKey = "key123";
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.TRANSFER, idempotencyKey, 1L, 2L);

        Transaction existingTx = new Transaction();
        existingTx.setId(10L);
        existingTx.setStatus(TransactionStatus.COMPLETED);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(transactionalRepository.findByUserIdAndIdempotencyKey(1L, idempotencyKey)).thenReturn(Optional.of(existingTx));
        when(transactionMapper.toDto(existingTx)).thenReturn(new TransactionResponse(10L, TransactionType.TRANSFER, TransactionStatus.COMPLETED, 
                new BigDecimal("100.00"), BigDecimal.ZERO, 1L, 2L, Currency.USD, LocalDateTime.now()));

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        verify(transactionLogger, never()).createPending(any(), any(), any(), any());
    }

    @Test
    void transfer_InsufficientFunds_Fails() {
        String idempotencyKey = "key123";
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("2000.00"), TransactionType.TRANSFER, idempotencyKey, 1L, 2L);

        Transaction pendingTx = new Transaction();
        pendingTx.setId(10L);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(transactionalRepository.findByUserIdAndIdempotencyKey(1L, idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByIdAndStatusForUpdate(1L, AccountStatus.ACTIVE)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdAndStatusForUpdate(2L, AccountStatus.ACTIVE)).thenReturn(Optional.of(targetAccount));
        when(transactionLogger.createPending(any(), any(), any(), any())).thenReturn(pendingTx);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> transactionService.transfer(request));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transactionLogger).markAsFailed(eq(10L), anyString());
    }

    @Test
    void deposit_Success() {
        String idempotencyKey = "key123";
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.DEPOSIT, idempotencyKey, null, 2L);

        Transaction pendingTx = new Transaction();
        pendingTx.setId(10L);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(transactionalRepository.findByUserIdAndIdempotencyKey(1L, idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findbyIdAndUserAndStatusForUpdate(2L, testUser, AccountStatus.ACTIVE)).thenReturn(Optional.of(targetAccount));
        when(transactionLogger.createPending(isNull(), eq(targetAccount), eq(request), eq(idempotencyKey)))
                .thenReturn(pendingTx);
        when(transactionalRepository.findById(10L)).thenReturn(Optional.of(pendingTx));
        when(transactionMapper.toDto(any())).thenReturn(mock(TransactionResponse.class));

        transactionService.transfer(request);

        assertEquals(new BigDecimal("600.00"), targetAccount.getBalance());
    }
}
