package com.learn.bankingapi.repository;

import com.learn.bankingapi.entity.Transaction;
import com.learn.bankingapi.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TransactionalRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Modifying
    @Query("""
        update Transaction t 
        set t.status = :status, t.message = :message 
        where t.id = :id
    """)
    void updateStatus(long id, TransactionStatus status, String message);
}
