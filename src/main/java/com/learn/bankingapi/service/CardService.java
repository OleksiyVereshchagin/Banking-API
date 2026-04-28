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
     * Creates a new card associated with a specified account.
     *
     * @param accountId The ID of the account to which the card will be linked.
     * @param request   The request payload containing details for creating the card, such as card type.
     * @return A response object containing details of the created card.
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the account does not exist or does not belong to the user
     *         - 400 BAD_REQUEST if the account is not ACTIVE or the card type is missing in the request
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
     * Retrieves a list of cards associated with a specific account.
     * The method behavior varies based on the role of the current user:
     * - ADMINs can access all cards for the account, including CLOSED cards.
     * - Regular USERS can only access active or frozen cards for the account.
     *
     * @param accountId the ID of the account for which the cards are being retrieved
     * @return a list of {@code CardResponse} objects representing the cards associated with the account
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the account does not exist or the user lacks access to it
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
                // Filter logic: ADMINs can see all cards (including CLOSED),
                // while regular USERs should only see active or frozen cards.
                .filter(card -> user.getRole() == ADMIN || card.getStatus() != CardStatus.CLOSED)
                .map(cardMapper::toDto)
                .toList();
    }

    /**
     * Retrieves the details of a card for the given card ID and the current user.
     *
     * @param cardId The unique identifier of the card to be retrieved.
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the card does not exist or the user lacks access to it
     */
    public CardResponse getCardDetails(long cardId) {

        User user = currentUserProvider.getCurrentUser();
        Card card = getCardOrThrow(cardId, user);

        return cardMapper.toDto(card);
    }

    /**
     * Updates the status of a card based on the provided request and card ID.
     *
     * @param request an object containing the new status for the card and any additional
     *                information necessary for the update
     * @param cardId the unique identifier of the card to be updated
     * @return a {@code CardResponse} object representing the updated card status
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the card does not exist or user lacks access
     *         - 403 FORBIDDEN if the user role is not allowed to perform this status change
     *         - 400 BAD_REQUEST if trying to close via PATCH or if status transition is invalid
     *         - 422 UNPROCESSABLE_ENTITY if the account is inactive or the card's current state is immutable
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
     * Deletes a card by marking its status as CLOSED.
     *
     * @param cardId the unique identifier of the card to be deleted
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the card does not exist or the user lacks access to it
     *         - 422 UNPROCESSABLE_ENTITY if the associated account is not active (only for non-ADMIN users)
     *         - 400 BAD_REQUEST if the card is already in CLOSED status
     */
    public void deleteCard(long cardId) {

        User user = currentUserProvider.getCurrentUser();
        Card card = getCardOrThrow(cardId, user);

        validateAccountActive(user, card);
        validateAlreadyClosed(card);

        card.setStatus(CardStatus.CLOSED);
        cardRepository.save(card);
    }

    // Helper Methods

    /**
     * Validate that the user can operate with a card based on account status.
     * ADMIN bypasses this rule.
     */
    private void validateAccountActive(User user, Card card) {

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (card.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Account is not active");
        }
    }



    /**
     * Prevent PATCH-based closing (must use DELETE)
     */
    private void validateClosedViaPatch(UpdateCardStatusRequest request) {

        if (request.status() == CardStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use DELETE endpoint to close card");
        }
    }

    /**
     * Block changes for final states
     */
    private void validateImmutableState(Card card) {

        if (Set.of(CardStatus.BLOCKED, CardStatus.EXPIRED, CardStatus.CLOSED)
                .contains(card.getStatus())) {

            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "card status is immutable");
        }
    }

    /**
     * Prevent no-op updates (same status)
     */
    private void validateNoOp(Card card, UpdateCardStatusRequest request) {

        if (card.getStatus() == request.status()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is already set");
        }
    }

    /**
     * Role-based status update rules dispatcher
     */
    private void validateRoleRules(User user, Card card, CardStatus newStatus) {

        switch (user.getRole()) {

            case USER -> validateUserRules(card, newStatus);

            case ADMIN -> validateAdminRules(card, newStatus);

            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role not allowed");
        }
    }

    /**
     * USER can only freeze ACTIVE card
     */
    private void validateUserRules(Card card, CardStatus newStatus) {

        if (newStatus != CardStatus.FROZEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USER can only freeze card");
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only ACTIVE card can be frozen");
        }
    }

    /**
     * ADMIN rules for activation flow
     */
    private void validateAdminRules(Card card, CardStatus newStatus) {

        if (newStatus == CardStatus.ACTIVE &&
                card.getStatus() != CardStatus.FROZEN) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only FROZEN card can be activated");
        }
    }

    /**
     * Prevent double delete operation
     */
    private void validateAlreadyClosed(Card card) {

        if (card.getStatus() == CardStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card is already closed");
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