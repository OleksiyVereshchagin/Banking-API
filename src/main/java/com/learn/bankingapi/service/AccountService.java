package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.request.account.CreateAccountRequest;
import com.learn.bankingapi.dto.request.account.UpdateAccountStatusRequest;
import com.learn.bankingapi.dto.response.account.AccountContainerResponse;
import com.learn.bankingapi.dto.response.account.AccountEditStatusResponse;
import com.learn.bankingapi.dto.response.account.AccountResponse;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.AccountMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import com.learn.bankingapi.utils.IBANGenerator;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Transactional
public class AccountService {
    private final CurrentUserProvider currentUser;
    private final IBANGenerator ibanGenerator;
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;

    public AccountService(CurrentUserProvider currentUser, IBANGenerator ibanGenerator, AccountMapper accountMapper, AccountRepository accountRepository) {
        this.currentUser = currentUser;
        this.ibanGenerator = ibanGenerator;
        this.accountMapper = accountMapper;
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(CreateAccountRequest request){
        if (request.currency() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is required");
        }

        User user = currentUser.getCurrentUser();
        Account account = new Account();

        account.setCurrency(request.currency());
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);

        String iban;
        int attempts = 0;
        do {
            if (attempts++ > 5) {
                throw new IllegalStateException("Failed to generate unique IBAN");
            }
            iban = ibanGenerator.generate();
        } while (accountRepository.existsByIban(iban));

        account.setIban(iban);

        return accountMapper.toDto(accountRepository.save(account));
    }

    public AccountContainerResponse getAccounts(){
        User user = currentUser.getCurrentUser();

        List<Account> accounts = accountRepository.findAllByUser(user);

        List <AccountResponse> accountResponseList = accounts
                .stream()
                .map(accountMapper::toDto)
                .toList();

        return new AccountContainerResponse(accountResponseList);
    }

    public AccountResponse getAccountDetails(long id){
        User user = currentUser.getCurrentUser();

        Account account = accountRepository.findAccountByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return accountMapper.toDto(account);
    }

    public AccountEditStatusResponse updateAccountStatus(long id, UpdateAccountStatusRequest request) {

        User user = currentUser.getCurrentUser();
        UserRole role = user.getRole();
        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User role is not defined"
            );
        }

        Account account;
        if (role == UserRole.ADMIN) {
            account = accountRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Account not found"
                    ));
        } else {
            account = accountRepository.findAccountByIdAndUser(id, user)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Account not found"
                    ));
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account is CLOSED and cannot be modified"
            );
        }

        if (request.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status is required"
            );
        }
        AccountStatus newStatus = request.status();

        if (account.getStatus() == newStatus) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New status must be different from the current status"
            );
        }

        if (newStatus == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transition to ACTIVE status is not allowed"
            );
        }

        switch (role) {

            case USER -> {
                if (newStatus != AccountStatus.CLOSED) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "You do not have permission to perform this status change"
                    );
                }

                if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Account balance must be zero to close the account"
                    );
                }

                account.setStatus(AccountStatus.CLOSED);
            }

            case ADMIN -> {
                if (newStatus != AccountStatus.BLOCKED && newStatus != AccountStatus.CLOSED) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Admins can only block or close accounts"
                    );
                }

                account.setStatus(newStatus);
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Role is not allowed to change account status"
            );
        }

        accountRepository.save(account);
        return accountMapper.toStatusUpdateDto(account, LocalDateTime.now());
    }
}
