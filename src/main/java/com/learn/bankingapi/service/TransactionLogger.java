package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.dto.request.transaction.CreateTransactionRequest;
import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;
import com.learn.bankingapi.repository.TransactionalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class TransactionLogger {

    private final TransactionalRepository transactionalRepository;

    public TransactionLogger(TransactionalRepository transactionalRepository) {
        this.transactionalRepository = transactionalRepository;
    }

    // CREATE
    @Transactional(propagation = REQUIRES_NEW)
    public Transaction createPending(
            Account source,
            Account target,
            CreateTransactionRequest request,
            String idempotencyKey
    ) {

        Long userId = resolveUserIdOrThrow(source, target, request.type());

        Transaction transaction = new Transaction();

        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCommission(BigDecimal.ZERO);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setFromAccount(source);
        transaction.setToAccount(target);
        transaction.setCurrency(resolveCurrencyOrThrow(source, target, request.type()));
        transaction.setUserId(userId);

        return transactionalRepository.saveAndFlush(transaction);
    }


//    @Transactional(propagation = REQUIRES_NEW)
//    public Transaction createPending(
//            Account source,
//            Account target,
//            CreateTransactionRequest request,
//            String idempotencyKey
//    ) {
//        Transaction transaction = new Transaction();
//
//        transaction.setType(request.type());
//        transaction.setAmount(request.amount());
//        transaction.setIdempotencyKey(idempotencyKey);
//        transaction.setCommission(BigDecimal.ZERO);
//        transaction.setStatus(TransactionStatus.PENDING);
//
//        // accounts (can be null depending on type)
//        transaction.setFromAccount(source);
//        transaction.setToAccount(target);
//
//        // currency (strict + safe)
//        transaction.setCurrency(resolveCurrencyOrThrow(source, target, request.type()));
//
//        // user (strict + safe)
//        transaction.setUserId(resolveUserIdOrThrow(source, target, request.type()));
//
//        return transactionalRepository.saveAndFlush(transaction);
//    }

    @Transactional(propagation = REQUIRES_NEW)
    public void markAsCompleted(long id) {
        transactionalRepository.updateStatus(id, TransactionStatus.COMPLETED, null);
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void markAsFailed(long id, String message) {
        transactionalRepository.updateStatus(id, TransactionStatus.FAILED, message);
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void markAsCancelled(long id, String message) {
        transactionalRepository.updateStatus(id, TransactionStatus.CANCELLED, message);
    }

    private void updateStatus(long id, TransactionStatus status, String message) {

        Transaction t = transactionalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        t.setStatus(status);

        if (message != null) {
            t.setMessage(message);
        }

        transactionalRepository.save(t);
    }

    // HELPERS
    private Currency resolveCurrencyOrThrow(Account source, Account target, TransactionType type) {

        return switch (type) {

            case DEPOSIT -> {
                if (target == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target required");
                yield target.getCurrency();
            }

            case WITHDRAW, TRANSFER -> {
                if (source == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source required");
                yield source.getCurrency();
            }
        };
    }

    private Long resolveUserIdOrThrow(Account source,
                                      Account target,
                                      TransactionType type) {

        return switch (type) {

            case DEPOSIT -> {
                if (target == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Target account is required for DEPOSIT"
                    );
                }
                yield target.getUser().getId();
            }

            case WITHDRAW -> {
                if (source == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Source account is required for WITHDRAW"
                    );
                }
                yield source.getUser().getId();
            }

            case TRANSFER -> {
                if (source == null || target == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Both source and target accounts are required for TRANSFER"
                    );
                }
                yield source.getUser().getId();
            }
        };
    }
}