package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Card;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.request.card.CreateCardRequest;
import com.learn.bankingapi.dto.request.card.UpdateCardStatusRequest;
import com.learn.bankingapi.dto.response.card.CardResponse;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.CardMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.repository.CardRepository;
import com.learn.bankingapi.utils.AccountAccessValidator;
import com.learn.bankingapi.utils.CardNumberGenerator;
import com.learn.bankingapi.utils.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static com.learn.bankingapi.enums.UserRole.ADMIN;


@Service
@Transactional
public class CardService {
    private final CardRepository cardRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final AccountRepository accountRepository;
    private final CardMapper cardMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AccountAccessValidator accountAccessValidator;

    public CardService(CardRepository cardRepository, CardNumberGenerator cardNumberGenerator, AccountRepository accountRepository, CardMapper cardMapper, CurrentUserProvider currentUserProvider, AccountAccessValidator accountAccessValidator) {
        this.cardRepository = cardRepository;
        this.cardNumberGenerator = cardNumberGenerator;
        this.accountRepository = accountRepository;
        this.cardMapper = cardMapper;
        this.currentUserProvider = currentUserProvider;
        this.accountAccessValidator = accountAccessValidator;
    }

    /**
     * Create new card for account
     */
    public CardResponse createCard(long accountId, CreateCardRequest request) {

        User user = currentUserProvider.getCurrentUser();

        Account account = accountRepository.findAccountByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));

        accountAccessValidator.validateCanOperate(account);

        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card type is required");
        }

        Card card = new Card();

        String cardNumber;
        do {
            cardNumber = cardNumberGenerator.generate();
        } while (cardRepository.existsByCardNumber(cardNumber));

        boolean existsDefault = cardRepository.existsByAccount_IdAndIsDefaultTrue(accountId);

        card.setDefault(!existsDefault);
        card.setCardNumber(cardNumber);
        card.setType(request.type());
        card.setAccount(account);

        return cardMapper.toDto(cardRepository.save(card));
    }

    /**
     * Get all cards by account (ADMIN sees all, USER hides CLOSED)
     */
    public List<CardResponse> getCardsByAccount(long accountId) {

        User user = currentUserProvider.getCurrentUser();

        Account account = (user.getRole() == ADMIN)
                ? accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"))
                : accountRepository.findAccountByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return cardRepository.findAllByAccount(account)
                .stream()
                .filter(card -> user.getRole() == ADMIN || card.getStatus() != CardStatus.CLOSED)
                .map(cardMapper::toDto)
                .toList();
    }

    /**
     * Get single card details (with access control)
     */
    public CardResponse getCardDetails(long cardId) {

        User user = currentUserProvider.getCurrentUser();
        Card card = getCardOrThrow(cardId, user);

        return cardMapper.toDto(card);
    }

    /**
     * Update card status with business rules validation
     */
    public CardResponse updateCardStatus(UpdateCardStatusRequest request, long cardId) {

        User user = currentUserProvider.getCurrentUser();
        Card card = getCardOrThrow(cardId, user);

        validateAccountActive(user, card);
        validateClosedViaPatch(request);
        validateImmutableState(card);
        validateNoOp(card, request);
        validateRoleRules(user, card, request.status());

        card.setStatus(request.status());
        cardRepository.save(card);

        return cardMapper.toDto(card);
    }

    /**
     * Soft delete card (sets status CLOSED)
     */
    public void deleteCard(long cardId) {

        User user = currentUserProvider.getCurrentUser();
        Card card = getCardOrThrow(cardId, user);

        validateAccountActive(user, card);
        validateAlreadyClosed(card);

        card.setStatus(CardStatus.CLOSED);
        cardRepository.save(card);
    }


    /**
     * Validate that user can operate with card based on account status.
     * ADMIN bypasses this rule.
     */
    private void validateAccountActive(User user, Card card) {

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (card.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Account is not active"
            );
        }
    }

    /**
     * Prevent double delete operation
     */
    private void validateAlreadyClosed(Card card) {

        if (card.getStatus() == CardStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Card is already closed"
            );
        }
    }

    /**
     * Prevent PATCH-based closing (must use DELETE)
     */
    private void validateClosedViaPatch(UpdateCardStatusRequest request) {

        if (request.status() == CardStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use DELETE endpoint to close card"
            );
        }
    }

    /**
     * Block changes for final states
     */
    private void validateImmutableState(Card card) {

        if (Set.of(CardStatus.BLOCKED, CardStatus.EXPIRED, CardStatus.CLOSED)
                .contains(card.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "card status is immutable"
            );
        }
    }

    /**
     * Prevent no-op updates (same status)
     */
    private void validateNoOp(Card card, UpdateCardStatusRequest request) {

        if (card.getStatus() == request.status()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status is already set"
            );
        }
    }

    /**
     * Role-based status update rules dispatcher
     */
    private void validateRoleRules(User user, Card card, CardStatus newStatus) {

        switch (user.getRole()) {

            case USER -> validateUserRules(card, newStatus);

            case ADMIN -> validateAdminRules(card, newStatus);

            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "role not allowed"
            );
        }
    }

    /**
     * USER can only freeze ACTIVE card
     */
    private void validateUserRules(Card card, CardStatus newStatus) {

        if (newStatus != CardStatus.FROZEN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "USER can only freeze card"
            );
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "only ACTIVE card can be frozen"
            );
        }
    }

    /**
     * ADMIN rules for activation flow
     */
    private void validateAdminRules(Card card, CardStatus newStatus) {

        if (newStatus == CardStatus.ACTIVE &&
                card.getStatus() != CardStatus.FROZEN) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "only FROZEN card can be activated"
            );
        }
    }

    /**
     * Fetch card by id with access control
     */
    private Card getCardOrThrow(long cardId, User user) {

        return (user.getRole() == UserRole.ADMIN)
                ? cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "card not found"))
                : cardRepository.findByIdAndAccountUserId(cardId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "card not found"));
    }
}