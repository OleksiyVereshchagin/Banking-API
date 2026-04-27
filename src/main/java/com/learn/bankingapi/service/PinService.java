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

    public void setPinForCard(SetPinRequest request, long id) {

        User user = currentUserProvider.getCurrentUser();

        Card card = getCardForUserOrThrow(id, user);

        validateCardAndAccountActive(card);
        validatePinNotSet(card);
        validatePinMatch(request);

        card.setPinHash(passwordEncoder.encode(request.pin()));

        cardRepository.save(card);
    }

    public void changePinForCard(ChangePinRequest request, long cardId) {

        User user = currentUserProvider.getCurrentUser();

        Card card = getCardForUserOrThrow(cardId, user);

        validateCardAndAccountActive(card);

        // 🔥 1. rate limit check
        rateLimitService.check(user.getId(), cardId);

        // 🔒 2. PIN must exist
        if (card.getPinHash() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PIN is not set"
            );
        }

        // 🔐 3. old PIN verification
        if (!passwordEncoder.matches(request.oldPin(), card.getPinHash())) {
            rateLimitService.registerFailure(user.getId(), cardId);

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid credentials"
            );
        }

        // 🔁 4. reset on success auth
        rateLimitService.reset(user.getId(), cardId);

        // 🧠 5. new != old
        if (request.oldPin().equals(request.newPin())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New PIN must be different from old PIN"
            );
        }

        // 💾 6. save new PIN
        card.setPinHash(passwordEncoder.encode(request.newPin()));

        cardRepository.save(card);
    }

    // ---------------- VALIDATION ----------------

    /**
     * Fetch card by id with ownership check (returns 404 if not found or чужа карта)
     */
    private Card getCardForUserOrThrow(long cardId, User user) {
        return cardRepository.findByIdAndAccountUserId(cardId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "card not found"
                ));
    }

    /**
     * Validate that both account and card are ACTIVE
     */
    private void validateCardAndAccountActive(Card card) {
        if (card.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Account is not active"
            );
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Card is not active"
            );
        }
    }


    /**
     * Ensure PIN is not already set for the card
     */
    private void validatePinNotSet(Card card) {
        if (card.getPinHash() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PIN already set"
            );
        }
    }

    /**
     * Validate that PIN and confirmation match
     */
    private void validatePinMatch(SetPinRequest request) {
        if (!request.pin().equals(request.confirmPin())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PIN not confirmed"
            );
        }
    }

}
