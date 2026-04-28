package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.Card;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.dto.request.pin.ChangePinRequest;
import com.learn.bankingapi.dto.request.pin.SetPinRequest;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.repository.CardRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PinService {

    private final CurrentUserProvider currentUserProvider;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final PinRateLimitService rateLimitService;

    public PinService(CurrentUserProvider currentUserProvider, CardRepository cardRepository, PasswordEncoder passwordEncoder, PinRateLimitService rateLimitService) {
        this.currentUserProvider = currentUserProvider;
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Sets a PIN for a card. The PIN must be exactly 4 digits long, and the provided
     * confirmation PIN must match the provided PIN. The card must belong to the current user,
     * and both the card and the associated account must be active.
     * If a PIN is already set for the card, an error will be thrown.
     *
     * @param request Contains the PIN and its confirmation that is to be set.
     *                Both must be 4-digit numeric strings.
     * @param id      The unique identifier of the card for which the PIN is to be set.
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the card does not exist or does not belong to the user
     *         - 422 UNPROCESSABLE_ENTITY if the card or its associated account is not active
     *         - 400 BAD_REQUEST if:
     *           - a PIN is already set for this card
     *           - the PIN confirmation does not match the provided PIN
     */
    public void setPinForCard(SetPinRequest request, long id) {
        User user = currentUserProvider.getCurrentUser();
        Card card = getCardForUserOrThrow(id, user);

        validateCardAndAccountActive(card);

        if (card.getPinHash() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN is already set");
        }

        if (!request.pin().equals(request.confirmPin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN confirmation does not match");
        }

        card.setPinHash(passwordEncoder.encode(request.pin()));
        cardRepository.save(card);
    }

    /**
     * Updates the PIN for a card after validating the current PIN and other conditions.
     *
     * @param request the request object containing the old and new PIN values
     * @param cardId  the identifier of the card whose PIN is to be changed
     * @throws ResponseStatusException with:
     *         - 404 NOT_FOUND if the card does not exist or does not belong to the user
     *         - 422 UNPROCESSABLE_ENTITY if the card or its associated account is not active
     *         - 400 BAD_REQUEST if:
     *           - a PIN is not set yet (use setPinForCard instead)
     *           - the new PIN is the same as the old one
     *         - 403 FORBIDDEN if the old PIN is invalid (this failure is registered in the rate limit service)
     *         - 429 TOO_MANY_REQUESTS if the operation is blocked due to 5 consecutive failed attempts
     */
    public void changePinForCard(ChangePinRequest request, long cardId) {
        User user = currentUserProvider.getCurrentUser();
        Card card = getCardForUserOrThrow(cardId, user);

        validateCardAndAccountActive(card);

        if (card.getPinHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN is not set yet");
        }

        //Rate Limiting
        rateLimitService.check(user.getId(), cardId);

        if (!passwordEncoder.matches(request.oldPin(), card.getPinHash())) {
            rateLimitService.registerFailure(user.getId(), cardId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid current PIN");
        }

        //Reset limit on success auth
        rateLimitService.reset(user.getId(), cardId);

        if (request.oldPin().equals(request.newPin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New PIN must be different from the old one");
        }

        card.setPinHash(passwordEncoder.encode(request.newPin()));
        cardRepository.save(card);
    }

    // Helper Methods

    private Card getCardForUserOrThrow(long cardId, User user) {
        return cardRepository.findByIdAndAccountUserId(cardId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "card not found"));
    }

    private void validateCardAndAccountActive(Card card) {
        if (card.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Account is not active");
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Card is not active");
        }
    }
}
