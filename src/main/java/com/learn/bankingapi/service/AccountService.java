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

    private static final int MAX_IBAN_ATTEMPTS = 15;

    public AccountService(CurrentUserProvider currentUser, IBANGenerator ibanGenerator, AccountMapper accountMapper, AccountRepository accountRepository) {
        this.currentUser = currentUser;
        this.ibanGenerator = ibanGenerator;
        this.accountMapper = accountMapper;
        this.accountRepository = accountRepository;
    }

    /**
     * Creates a new account for the current user with the provided details.
     *
     * @param request the request object containing details for the new account, such as currency
     * @return an AccountResponse object containing details of the newly created account
     * @throws ResponseStatusException with:
     *         - 422 UNPROCESSABLE_ENTITY if the system fails to generate a unique IBAN
     *           after several attempts
     */
    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = currentUser.getCurrentUser();

        String iban = generateUniqueIban();

        Account account = new Account();
        account.setCurrency(request.currency());
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);
        account.setIban(iban);

        return accountMapper.toDto(accountRepository.save(account));
    }


    /**
     * Retrieves a container of account responses associated with the currently logged-in user.
     *
     * @return an AccountContainerResponse containing a list of account responses for the current user.
     */
    public AccountContainerResponse getAccounts() {
        User user = currentUser.getCurrentUser();

        List<AccountResponse> accountResponses = accountRepository.findAllByUser(user)
                .stream()
                .map(accountMapper::toDto)
                .toList();

        return new AccountContainerResponse(accountResponses);
    }

    /**
     * Retrieves the account details for the specified account ID associated with the current user.
     *
     * @param id the unique identifier of the account to retrieve
     * @return an AccountResponse object containing the account details
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the account does not exist or the user lacks access to it
     */
    public AccountResponse getAccountDetails(long id){
        User user = currentUser.getCurrentUser();

        Account account = findAccountForUser(id, user);

        return accountMapper.toDto(account);
    }

    /**
     * Updates the status of an account for the currently logged-in user.
     *
     * @param id The unique identifier of the account to update.
     * @param request The request object containing the new account status.
     * @return An AccountEditStatusResponse object representing the result of the update operation.
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the account does not exist or access is denied
     *         - 400 BAD_REQUEST if:
     *           - the account is already CLOSED
     *           - the new status is the same as current
     *           - trying to transition to ACTIVE status
     *           - trying to close an account with a non-zero balance
     *         - 403 FORBIDDEN if:
     *           - a regular USER tries to do anything other than CLOSE the account
     *           - an ADMIN tries to set a status other than BLOCKED or CLOSED
     */
    public AccountEditStatusResponse updateAccountStatus(long id, UpdateAccountStatusRequest request) {
        User user = currentUser.getCurrentUser();
        Account account = findAccountForUser(id, user);

        validateStatusChange(account, request.status(), user.getRole());

        account.setStatus(request.status());
        accountRepository.save(account);

        return accountMapper.toStatusUpdateDto(account, LocalDateTime.now());
    }

    // Helper Methods

    private Account findAccountForUser(long id, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return accountRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        }
        return accountRepository.findAccountByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found or access denied"));
    }

    private void validateStatusChange(Account account, AccountStatus newStatus, UserRole role) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is CLOSED and cannot be modified");
        }

        if (account.getStatus() == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New status must be different");
        }

        if (newStatus == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transition to ACTIVE is not allowed");
        }

        if (role == UserRole.USER) {
            validateUserPermissions(account, newStatus);
        } else if (role == UserRole.ADMIN) {
            validateAdminPermissions(newStatus);
        }
    }

    private void validateUserPermissions(Account account, AccountStatus newStatus) {
        if (newStatus != AccountStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users can only CLOSE their accounts");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance must be zero to close account");
        }
    }

    private void validateAdminPermissions(AccountStatus newStatus) {
        if (newStatus != AccountStatus.BLOCKED && newStatus != AccountStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins can only block or close accounts");
        }
    }

    private String generateUniqueIban() {
        String iban;
        int attempts = 0;
        do {
            if (attempts++ > MAX_IBAN_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not generate unique IBAN, please try again");            }
            iban = ibanGenerator.generate();
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}
