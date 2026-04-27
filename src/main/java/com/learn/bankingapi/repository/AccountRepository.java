package com.learn.bankingapi.repository;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByIban(String iban);

    List<Account> findAllByUser(User user);

    Optional<Account> findAccountByIdAndUser(Long id, User user);

    boolean existsByIdAndUserAndStatus(Long id, User user, AccountStatus status);

    Optional<Account> findByIdAndStatus(Long id, AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select a from Account a 
    where a.id = :id and a.status = :status
""")
    Optional<Account> findByIdAndStatusForUpdate(Long id, AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id and a.user = :user and a.status = :status")
    Optional<Account> findbyIdAndUserAndStatusForUpdate(Long id, User user, AccountStatus status);
}
