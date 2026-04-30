package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.AccountMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import com.learn.bankingapi.utils.IBANGenerator;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IBANGenerator ibanGenerator;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(UserRole.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setRole(UserRole.ADMIN);
    }

    @Test
    void createAccount_Success() {
        CreateAccountRequest request = new CreateAccountRequest(Currency.USD);
        String generatedIban = "UA1234567890";
        Account account = new Account();
        account.setIban(generatedIban);
        account.setCurrency(Currency.USD);
        account.setUser(testUser);
        account.setStatus(AccountStatus.ACTIVE);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(ibanGenerator.generate()).thenReturn(generatedIban);
        when(accountRepository.existsByIban(generatedIban)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(any(Account.class))).thenReturn(new AccountResponse(1L, BigDecimal.ZERO, generatedIban, Currency.USD, AccountStatus.ACTIVE, LocalDateTime.now()));

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(generatedIban, response.iban());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_Failure_IbanGeneration() {
        CreateAccountRequest request = new CreateAccountRequest(Currency.USD);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(ibanGenerator.generate()).thenReturn("DUPLICATE");
        when(accountRepository.existsByIban("DUPLICATE")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> accountService.createAccount(request));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Could not generate unique IBAN"));
    }

    @Test
    void getAccounts_Success() {
        Account account = new Account();
        account.setUser(testUser);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAllByUser(testUser)).thenReturn(List.of(account));
        when(accountMapper.toDto(account)).thenReturn(new AccountResponse(1L, BigDecimal.ZERO, "IBAN", Currency.USD, AccountStatus.ACTIVE, LocalDateTime.now()));

        AccountContainerResponse response = accountService.getAccounts();

        assertNotNull(response);
        assertEquals(1, response.accounts().size());
    }

    @Test
    void getAccountDetails_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setUser(testUser);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(new AccountResponse(1L, BigDecimal.ZERO, "IBAN", Currency.USD, AccountStatus.ACTIVE, LocalDateTime.now()));

        AccountResponse response = accountService.getAccountDetails(1L);

        assertNotNull(response);
        verify(accountRepository).findAccountByIdAndUser(1L, testUser);
    }

    @Test
    void getAccountDetails_NotFound() {
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.getAccountDetails(1L));
    }

    @Test
    void updateAccountStatus_User_Close_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(testUser);
        account.setIban("IBAN");
        account.setCurrency(Currency.USD);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.CLOSED);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(account));
        when(accountMapper.toStatusUpdateDto(any(), any())).thenReturn(new AccountEditStatusResponse(1L, "IBAN", Currency.USD, AccountStatus.CLOSED, LocalDateTime.now()));

        AccountEditStatusResponse response = accountService.updateAccountStatus(1L, request);

        assertEquals(AccountStatus.CLOSED, account.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccountStatus_User_Forbidden_Status() {
        Account account = new Account();
        account.setId(1L);
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(testUser);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.BLOCKED);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(account));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> accountService.updateAccountStatus(1L, request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void updateAccountStatus_User_BadRequest_NonZeroBalance() {
        Account account = new Account();
        account.setId(1L);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.TEN);
        account.setUser(testUser);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.CLOSED);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(account));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> accountService.updateAccountStatus(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateAccountStatus_Admin_Block_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setStatus(AccountStatus.ACTIVE);
        account.setIban("IBAN");
        account.setCurrency(Currency.USD);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.BLOCKED);
        when(currentUserProvider.getCurrentUser()).thenReturn(adminUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountMapper.toStatusUpdateDto(any(), any())).thenReturn(new AccountEditStatusResponse(1L, "IBAN", Currency.USD, AccountStatus.BLOCKED, LocalDateTime.now()));

        accountService.updateAccountStatus(1L, request);

        assertEquals(AccountStatus.BLOCKED, account.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccountStatus_BadRequest_AlreadyClosed() {
        Account account = new Account();
        account.setStatus(AccountStatus.CLOSED);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(account));

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.BLOCKED);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> accountService.updateAccountStatus(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
