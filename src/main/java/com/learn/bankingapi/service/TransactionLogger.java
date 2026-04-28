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

/**
 * Service for logging transactions and managing their lifecycle statuses.
 * All methods use {@code REQUIRES_NEW} propagation to ensure that transaction logs
 * are persisted independently of the main business transaction's success or failure.
 */
@Service
public class TransactionLogger {

    private final TransactionalRepository transactionalRepository;

    public TransactionLogger(TransactionalRepository transactionalRepository) {
        this.transactionalRepository = transactionalRepository;
    }

    /**
     * Creates a new transaction record with PENDING status.
     *
     * @param source         The account from which funds are being withdrawn (null for DEPOSIT).
     * @param target         The account to which funds are being deposited (null for WITHDRAW).
     * @param request        The request containing transaction details like type and amount.
     * @param idempotencyKey A unique key to prevent duplicate transaction processing.
     * @return The created {@link Transaction} entity.
     * @throws ResponseStatusException with 400 BAD_REQUEST if required accounts for the
     *                                 specified transaction type are missing.
     */
    @Transactional(propagation = REQUIRES_NEW)
    public Transaction createPending(Account source, Account target, CreateTransactionRequest request, String idempotencyKey) {
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


    /**
     * Marks a transaction as successfully COMPLETED.
     *
     * @param id The unique identifier of the transaction.
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void markAsCompleted(long id) {
        transactionalRepository.updateStatus(id, TransactionStatus.COMPLETED, null);
    }

    /**
     * Marks a transaction as FAILED with a specific error message.
     *
     * @param id      The unique identifier of the transaction.
     * @param message The reason why the transaction failed.
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void markAsFailed(long id, String message) {
        transactionalRepository.updateStatus(id, TransactionStatus.FAILED, message);
    }

    /**
     * Marks a transaction as CANCELLED (e.g., by user or system before processing).
     *
     * @param id      The unique identifier of the transaction.
     * @param message The reason for cancellation.
     */
    @Transactional(propagation = REQUIRES_NEW)
    public void markAsCancelled(long id, String message) {
        transactionalRepository.updateStatus(id, TransactionStatus.CANCELLED, message);
    }

    // Helper Methods

    /**
     * Determines the transaction currency based on the transaction type and involved accounts.
     *
     * @throws ResponseStatusException with 400 BAD_REQUEST if the required account for currency resolution is missing.
     */
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

    /**
     * Identifies the user associated with the transaction for auditing purposes.
     *
     * @throws ResponseStatusException with 400 BAD_REQUEST if mandatory accounts for the given
     *                                 transaction type (DEPOSIT, WITHDRAW, TRANSFER) are not provided.
     */
    private Long resolveUserIdOrThrow(Account source, Account target, TransactionType type) {
        return switch (type) {

            case DEPOSIT -> {
                if (target == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target account is required for DEPOSIT");
                }
                yield target.getUser().getId();
            }

            case WITHDRAW -> {
                if (source == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source account is required for WITHDRAW");
                }
                yield source.getUser().getId();
            }

            case TRANSFER -> {
                if (source == null || target == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both source and target accounts are required for TRANSFER");
                }
                yield source.getUser().getId();
            }
        };
    }
}