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

    public TransactionResponse transfer(CreateTransactionRequest request) {

        User user = currentUserProvider.getCurrentUser();

        String idempotencyKey = request.idempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency key is required");
        }

        var existing = transactionalRepository
                .findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);

        if (existing.isPresent()) {
            return transactionMapper.toDto(existing.get());
        }

        // 2. validation (pure)
        isRequestValid(request);

        Account source = null;
        Account target = null;

        switch (request.type()) {
            case TRANSFER -> {
                Long fromId = request.fromAccountId();
                Long toId = request.toAccountId();

                // 1. Визначаємо черговість блокування за ID
                Long firstId = Math.min(fromId, toId);
                Long secondId = Math.max(fromId, toId);

                // 2. Блокуємо спочатку менший ID, потім більший
                // Примітка: використовуй метод, який робить SELECT FOR UPDATE
                Account firstAccount = accountRepository.findByIdAndStatusForUpdate(firstId, AccountStatus.ACTIVE)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + firstId + " not found"));

                Account secondAccount = accountRepository.findByIdAndStatusForUpdate(secondId, AccountStatus.ACTIVE)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + secondId + " not found"));

                // 3. Тепер правильно розставляємо source та target для подальшої логіки execute()
                source = (fromId.equals(firstId)) ? firstAccount : secondAccount;
                target = (toId.equals(secondId)) ? secondAccount : firstAccount;

                // Важливо: перевір, чи source належить користувачу (user),
                // як ти робив у методі getActiveAccount
                if (source.getUser() == null || !Objects.equals(source.getUser().getId(), user.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own the source account");
                }
            }
            case DEPOSIT -> target = getActiveAccount(request.toAccountId(), user,
                    "Target account not found");
            case WITHDRAW -> source = getActiveAccount(request.fromAccountId(), user,
                    "Source account not found");
        }

        Transaction transaction;
        try {
            // 3. Спроба створити лог (нова транзакція)
            transaction = transactionLogger.createPending(source, target, request, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // ⚡️ RACE CONDITION DETECTED!
            // Якщо ми тут, значить інший потік встиг створити запис між кроком 1 і 3.
            // Просто дістаємо те, що створив інший потік.
            return transactionalRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                    .map(transactionMapper::toDto)
                    .orElseThrow(() -> e); // Якщо не знайшли - значить помилка була в іншому
        }

        try {
            execute(request, source, target);
            transactionLogger.markAsCompleted(transaction.getId());

            return transactionalRepository.findById(transaction.getId())
                    .map(transactionMapper::toDto)
                    .orElseThrow();
        } catch (ResponseStatusException e) {

            transactionLogger.markAsFailed(
                    transaction.getId(),
                    e.getReason() != null ? e.getReason() : "Business error"
            );

            throw e;

        } catch (Exception e) {

            transactionLogger.markAsCancelled(
                    transaction.getId(),
                    "Unexpected error"
            );

            throw e;
        }
    }

    public PageResponse<TransactionResponse> getTransactions(TransactionFilter filter, Pageable pageable) {

        User user = currentUserProvider.getCurrentUser();
        validateFilter(filter);

        // Створюємо специфікацію "на льоту"
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // 1. Умова приналежності транзакції користувачу (Source АБО Target)
            // SQL: WHERE (source_account.user_id = ? OR target_account.user_id = ?)
            // Обмеження за поточним юзером
            Predicate userIsSender = cb.equal(root.join("fromAccount", JoinType.LEFT).get("user").get("id"), user.getId());
            Predicate userIsReceiver = cb.equal(root.join("toAccount", JoinType.LEFT).get("user").get("id"), user.getId());
            predicates.add(cb.or(userIsSender, userIsReceiver));

            // 3. Викликаємо твої окремі методи-цеглинки
            // Кожен метод додає в список predicates нові умови
            applyAmountFilter(filter, cb, root, predicates);
            applyDateFilter(filter, cb, root, predicates); // додай сюди cb та root!
            applyStatusFilter(filter, root, predicates);
            applyTypeFilter(filter, root, predicates);
            applyCurrencyFilter(filter, root, predicates);


            // 4. Склеюємо всі цеглинки в одну стіну за допомогою AND
            // SQL: WHERE amount > 10 AND status = 'COMPLETED' ...
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page <Transaction> page = transactionalRepository.findAll(spec, pageable);
        // Мапимо в DTO (твій PageResponse)
        return transactionMapper.toPageResponse(page);
    }

    public PageResponse<TransactionResponse> getTransactionsByAccount(Long accountId, TransactionFilter filter, Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();

        validateFilter(filter);
        validateAccountOwnership(accountId, user);

        // Створюємо специфікацію "на льоту"
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // 3. Викликаємо твої окремі методи-цеглинки
            // Кожен метод додає в список predicates нові умови
            applyAccountFilter(accountId, cb, root, predicates);
            applyAmountFilter(filter, cb, root, predicates);
            applyDateFilter(filter, cb, root, predicates); // додай сюди cb та root!
            applyStatusFilter(filter, root, predicates);
            applyTypeFilter(filter, root, predicates);
            applyCurrencyFilter(filter, root, predicates);


            // 4. Склеюємо всі цеглинки в одну стіну за допомогою AND
            // SQL: WHERE amount > 10 AND status = 'COMPLETED' ...
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page <Transaction> page = transactionalRepository.findAll(spec, pageable);
        // Мапимо в DTO (твій PageResponse)
        return transactionMapper.toPageResponse(page);

    }

    private void execute(CreateTransactionRequest request,
                         Account source,
                         Account target) {

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
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Currency mismatch"
                    );
                }

                source.setBalance(source.getBalance().subtract(request.amount()));
                target.setBalance(target.getBalance().add(request.amount()));
            }
        }
    }

    private void validateBalance(Account account, BigDecimal amount){
        if(account.getBalance().compareTo(amount) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }
    }

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
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported transaction type"
            );
        }
    }

    private Account getActiveAccount(Long id, User user, String message) {
        return accountRepository.findbyIdAndUserAndStatusForUpdate(id, user, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, message));
    }

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

    private void applyAmountFilter(
            TransactionFilter filter,
            CriteriaBuilder cb,
            Root<Transaction> root,
            List<Predicate> predicates
    ) {

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


    private void applyDateFilter(
            TransactionFilter filter,
            CriteriaBuilder cb,
            Root<Transaction> root,
            List<Predicate> predicates
    ) {

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

    private void applyStatusFilter(
            TransactionFilter filter,
            Root<Transaction> root,
            List<Predicate> predicates
    ) {
        if(filter.statuses() != null && !filter.statuses().isEmpty()) {
            predicates.add(root.get("status").in(filter.statuses()));
        }
    }

    private void applyTypeFilter(
            TransactionFilter filter,
            Root<Transaction> root,
            List<Predicate> predicates
    ) {
        if(filter.types() != null && !filter.types().isEmpty()) {
            predicates.add(root.get("type").in(filter.types()));
        }
    }

    private void applyCurrencyFilter(
            TransactionFilter filter,
            Root<Transaction> root,
            List<Predicate> predicates
    ) {
        if(filter.currencies() != null && !filter.currencies().isEmpty()) {
            predicates.add(root.get("currency").in(filter.currencies()));
        }
    }

    private void applyAccountFilter(
            long accountId,
            CriteriaBuilder cb,
            Root<Transaction> root,
            List<Predicate> predicates
    ){
        Predicate from = (cb.equal(root.get("fromAccount").get("id"), accountId));
        Predicate to = (cb.equal(root.get("toAccount").get("id"), accountId));
        predicates.add(cb.or(from, to));
    }


}
