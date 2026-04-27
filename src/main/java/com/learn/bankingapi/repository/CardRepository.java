package com.learn.bankingapi.repository;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsByAccount_IdAndIsDefaultTrue(Long accountId);
    boolean existsByCardNumber(String cardNumber);
    Optional<Card> findCardById(Long id);
    List<Card> findAllByAccount(Account account);
    Optional<Card> findByIdAndAccountUserId(Long cardId, Long userId);
}
