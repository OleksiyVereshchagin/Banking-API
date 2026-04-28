package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.filter.TransactionFilter;
import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.dto.response.transaction.PageResponse;
import com.learn.bankingapi.dto.response.transaction.TransactionResponse;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.mapper.TransactionMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.repository.TransactionalRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TransactionService {

    private final TransactionMapper transactionMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AccountRepository accountRepository;
    private final TransactionLogger transactionLogger;
    private final TransactionalRepository transactionalRepository;

    public TransactionService(TransactionMapper transactionMapper, CurrentUserProvider currentUserProvider, AccountRepository accountRepository, TransactionLogger transactionLogger, TransactionalRepository transactionalRepository) {
        this.transactionMapper = transactionMapper;
        this.currentUserProvider = currentUserProvider;
        this.accountRepository = accountRepository;
        this.transactionLogger = transactionLogger;
        this.transactionalRepository = transactionalRepository;
    }

    /**
     * Processes a financial transaction (TRANSFER, DEPOSIT, or WITHDRAW).
     * Logic flow:
     * 1. Idempotency Check: Returns existing transaction if the user already uses the idempotency key.
     * 2. Validation: Validates request parameters and account availability.
     * 3. Deadlock Prevention (for TRANSFER): Locks accounts in a strictly defined order (by ID)
     *    using SELECT FOR UPDATE to prevent circular wait conditions.
     * 4. Logging: Creates a PENDING record in a separate transaction.
     * 5. Execution: Updates account balances and marks the transaction as COMPLETED, FAILED, or CANCELLED.
     *
     * @param request The transaction request containing type, amount, accounts, and idempotency key.
     * @return A {@link TransactionResponse} representing the processed transaction.
     * @throws ResponseStatusException with:
     *         - 400 BAD_REQUEST if:
     *           - an idempotency key is missing
     *           - request validation fails (e.g., negative amount, missing accounts)
     *           - currency mismatch occurs during TRANSFER
     *           - account balance is not enough
     *         - 404 NOT_FOUND if involved accounts do not exist or are not ACTIVE
     *         - 403 FORBIDDEN if the user does not own the source account
     */
    public TransactionResponse transfer(CreateTransactionRequest request) {
        // 1. User identification and idempotency key verification
        User user = currentUserProvider.getCurrentUser();
        String idempotencyKey = request.idempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency key is required");
        }

        // Check if this request has already been processed to avoid duplication
        var existing = transactionalRepository
                .findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);

        if (existing.isPresent()) {
            return transactionMapper.toDto(existing.get());
        }

        // 2. Basic validation of the request structure (presence of required fields)
        isRequestValid(request);

        Account source = null;
        Account target = null;

        // 3. Account preparation and Deadlock Prevention
        switch (request.type()) {
            case TRANSFER -> {
                Long fromId = request.fromAccountId();
                Long toId = request.toAccountId();

                // Determine locking order by ID to avoid deadlock
                // Always lock accounts in the same order (from lower ID to higher ID)
                Long firstId = Math.min(fromId, toId);
                Long secondId = Math.max(fromId, toId);

                // Lock accounts in the database using SELECT FOR UPDATE in a strict order
                Account firstAccount = accountRepository.findByIdAndStatusForUpdate(firstId, AccountStatus.ACTIVE)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + firstId + " not found"));

                Account secondAccount = accountRepository.findByIdAndStatusForUpdate(secondId, AccountStatus.ACTIVE)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + secondId + " not found"));

                // Assign source and target roles according to the request
                source = (fromId.equals(firstId)) ? firstAccount : secondAccount;
                target = (toId.equals(secondId)) ? secondAccount : firstAccount;

                if (source.getUser() == null || !Objects.equals(source.getUser().getId(), user.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own the source account");
                }
            }
            case DEPOSIT -> target = getActiveAccount(request.toAccountId(), user,
                    "Target account not found");
            case WITHDRAW -> source = getActiveAccount(request.fromAccountId(), user,
                    "Source account not found");
        }

        // 4. Transaction logging (creating a record with PENDING status)
        Transaction transaction;
        try {
            // Executed in a separate transaction (REQUIRES_NEW), so the log remains even if the main logic fails
            transaction = transactionLogger.createPending(source, target, request, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // Race Condition handling: if a parallel thread managed to create the log first
            return transactionalRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                    .map(transactionMapper::toDto)
                    .orElseThrow(() -> e);
        }

        // 5. Execution of financial operation and status finalization
        try {
            processFinancialOperation(request, source, target);
            transactionLogger.markAsCompleted(transaction.getId());

            return transactionalRepository.findById(transaction.getId())
                    .map(transactionMapper::toDto)
                    .orElseThrow();
        } catch (ResponseStatusException e) {
            // Handling expected business errors
            transactionLogger.markAsFailed(
                    transaction.getId(),
                    e.getReason() != null ? e.getReason() : "Business error"
            );
            throw e;
        } catch (Exception e) {
            // Handling unexpected technical failures
            transactionLogger.markAsCancelled(
                    transaction.getId(),
                    "Unexpected error"
            );
            throw e;
        }
    }

    /**
     * Retrieves a paginated list of all transactions associated with the current user.
     * The result includes transactions where the user's accounts are either the source or the target.
     *
     * @param filter   Criteria for filtering
     * @param pageable Pagination and sorting information.
     * @return A {@link PageResponse} containing the filtered transaction data.
     * @throws ResponseStatusException with 400 BAD_REQUEST if the filter criteria are invalid
     *                                 (e.g., minAmount > maxAmount).
     */
    public PageResponse<TransactionResponse> getTransactions(TransactionFilter filter, Pageable pageable) {

        User user = currentUserProvider.getCurrentUser();
        validateFilter(filter);

        // Dynamic query building using JPA Specification
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // Security filter: Include transactions where the current user is either the sender or the receiver
            Predicate userIsSender = cb.equal(root.join("fromAccount", JoinType.LEFT).get("user").get("id"), user.getId());
            Predicate userIsReceiver = cb.equal(root.join("toAccount", JoinType.LEFT).get("user").get("id"), user.getId());
            predicates.add(cb.or(userIsSender, userIsReceiver));

            // Apply additional business filters
            applyAmountFilter(filter, cb, root, predicates);
            applyDateFilter(filter, cb, root, predicates); // додай сюди cb та root!
            applyStatusFilter(filter, root, predicates);
            applyTypeFilter(filter, root, predicates);
            applyCurrencyFilter(filter, root, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page <Transaction> page = transactionalRepository.findAll(spec, pageable);
        return transactionMapper.toPageResponse(page);
    }

    /**
     * Retrieves a paginated list of transactions for a specific account.
     *
     * @param accountId The ID of the account to fetch transactions for.
     * @param filter    Criteria for filtering the results.
     * @param pageable  Pagination and sorting information.
     * @return A {@link PageResponse} containing the transaction data for the specified account.
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the account does not exist or is not owned by the current user
     *         - 400 BAD_REQUEST if the filter criteria are invalid
     */
    public PageResponse<TransactionResponse> getTransactionsByAccount(Long accountId, TransactionFilter filter, Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();

        validateFilter(filter);
        validateAccountOwnership(accountId, user);

        // Створюємо специфікацію "на льоту"
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // Account filter: Transaction must involve the specified account (as source OR target)
            applyAccountFilter(accountId, cb, root, predicates);

            applyAmountFilter(filter, cb, root, predicates);
            applyDateFilter(filter, cb, root, predicates); // додай сюди cb та root!
            applyStatusFilter(filter, root, predicates);
            applyTypeFilter(filter, root, predicates);
            applyCurrencyFilter(filter, root, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page <Transaction> page = transactionalRepository.findAll(spec, pageable);
        return transactionMapper.toPageResponse(page);
    }

    // Helper Methods

    /**
     * Executes the financial operation by updating account balances.
     * This method modifies the state of the provided account entities based on the transaction type.
     * Logic flow:
     * 1. DEPOSIT: Increases the target account balance.
     * 2. WITHDRAW: Validates sufficient funds and decreases the source account balance.
     * 3. TRANSFER: Validates source balance, ensures currency compatibility between accounts,
     *    and performs a double-entry update (subtract from source, add to target).
     *
     * @param request The transaction details (type and amount).
     * @param source  The account funds are taken from (required for WITHDRAW and TRANSFER).
     * @param target  The account funds are sent to (required for DEPOSIT and TRANSFER).
     * @throws ResponseStatusException with:
     *         - 400 BAD_REQUEST if funds are not enough or if currencies do not match during a transfer.
     */
    private void processFinancialOperation(CreateTransactionRequest request, Account source, Account target) {
        switch (request.type()) {
            case DEPOSIT -> {
                target.setBalance(target.getBalance().add(request.amount()));
            }

            case WITHDRAW -> {
                validateBalance(source, request.amount());
                source.setBalance(source.getBalance().subtract(request.amount()));
            }

            case TRANSFER -> {
                validateBalance(source, request.amount());

                if (!source.getCurrency().equals(target.getCurrency())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
                }

                source.setBalance(source.getBalance().subtract(request.amount()));
                target.setBalance(target.getBalance().add(request.amount()));
            }
        }
    }

    /**
     * Checks if the account has enough balances for the transaction.
     * @throws ResponseStatusException with 400 BAD_REQUEST if funds are insufficient.
     */
    private void validateBalance(Account account, BigDecimal amount){
        if(account.getBalance().compareTo(amount) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
    }

    /**
     * Validates the request structure based on the transaction type.
     * Ensures that mandatory accounts are present and source/target rules are followed.
     */
    private void isRequestValid(CreateTransactionRequest request){

        if(request.type() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type is required");
        }

        if(request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        switch (request.type()){
            case TRANSFER -> {
                if(request.fromAccountId() == null || request.toAccountId() == null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both source and target accounts are required");
                }
                if(Objects.equals(request.fromAccountId(), request.toAccountId())){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From and to account cannot be the same");
                }
            }
            case DEPOSIT -> {
                if(request.toAccountId() == null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target account is required");
                }
                if(request.fromAccountId() != null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source account must be null for deposit");
                }
            }
            case WITHDRAW -> {
                if(request.fromAccountId() == null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source account is required");
                }
                if(request.toAccountId() != null){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target account must be null for withdraw");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported transaction type");
        }
    }

    private Account getActiveAccount(Long id, User user, String message) {
        return accountRepository.findbyIdAndUserAndStatusForUpdate(id, user, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }

    /**
     * Validates that the provided filter ranges (amount and date) are logically correct.
     */
    private void validateFilter(TransactionFilter filter){

        if(filter.amountMin() != null && filter.amountMax() != null && filter.amountMin().compareTo(filter.amountMax()) > 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount min must be less than amount max");
        }
        if(filter.createdAtFrom() != null && filter.createdAtTo() != null && filter.createdAtFrom().isAfter(filter.createdAtTo())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Created at from must be before created at to");
        }
    }

    private void validateAccountOwnership(Long accountId, User user){
        accountRepository.findAccountByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found or not owned by user"));
    }

    /**
     * Applies range filtering for the transaction amount.
     * Includes both minimum and maximum boundaries if they are provided in the filter.
     */
    private void applyAmountFilter(TransactionFilter filter, CriteriaBuilder cb, Root<Transaction> root, List<Predicate> predicates) {
        if (filter.amountMin() != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(
                            root.get("amount"),
                            filter.amountMin()
                    )
            );
        }

        if (filter.amountMax() != null) {
            predicates.add(
                    cb.lessThanOrEqualTo(
                            root.get("amount"),
                            filter.amountMax()
                    )
            );
        }
    }

    /**
     * Applies date range filtering for transaction creation time.
     *
     * Logic specifics:
     * 1. 'From' date: Includes the start of the specified day (00:00:00).
     * 2. 'To' date: Expands the range to include the entire end day by checking if
     *    the timestamp is strictly less than the start of the next day.
     */
    private void applyDateFilter(TransactionFilter filter, CriteriaBuilder cb, Root<Transaction> root, List<Predicate> predicates) {
        if (filter.createdAtFrom() != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            filter.createdAtFrom().atStartOfDay()
                    )
            );
        }

        if (filter.createdAtTo() != null) {
            predicates.add(
                    cb.lessThan(
                            root.get("createdAt"),
                            filter.createdAtTo().plusDays(1).atStartOfDay()
                    )
            );
        }
    }

    /**
     * Filters transactions by their current status (e.g., PENDING, COMPLETED, FAILED).
     * Uses an 'IN' clause if a list of statuses is provided.
     */
    private void applyStatusFilter(TransactionFilter filter, Root<Transaction> root, List<Predicate> predicates) {
        if(filter.statuses() != null && !filter.statuses().isEmpty()) {
            predicates.add(root.get("status").in(filter.statuses()));
        }
    }

    /**
     * Filters transactions by type (e.g., TRANSFER, DEPOSIT, WITHDRAW).
     * Multiple types can be selected simultaneously.
     */
    private void applyTypeFilter(TransactionFilter filter, Root<Transaction> root, List<Predicate> predicates) {
        if(filter.types() != null && !filter.types().isEmpty()) {
            predicates.add(root.get("type").in(filter.types()));
        }
    }

    /**
     * Filters transactions based on the currency used.
     * Supports multiple currencies via an 'IN' clause.
     */
    private void applyCurrencyFilter(TransactionFilter filter, Root<Transaction> root, List<Predicate> predicates) {
        if(filter.currencies() != null && !filter.currencies().isEmpty()) {
            predicates.add(root.get("currency").in(filter.currencies()));
        }
    }

    /**
     * Restricts the query to transactions where the specified account is involved.
     *
     * Logic:
     * A transaction is included if the account ID matches either the 'fromAccount'
     * or the 'toAccount' field (logical OR).
     */
    private void applyAccountFilter(long accountId, CriteriaBuilder cb, Root<Transaction> root, List<Predicate> predicates){
        Predicate from = (cb.equal(root.get("fromAccount").get("id"), accountId));
        Predicate to = (cb.equal(root.get("toAccount").get("id"), accountId));
        predicates.add(cb.or(from, to));
    }
}
