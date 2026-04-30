package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.pin.ChangePinRequest;
import com.learn.bankingapi.dto.request.pin.SetPinRequest;
import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Card;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.repository.CardRepository;
import com.learn.bankingapi.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PinRateLimitService rateLimitService;

    @InjectMocks
    private PinService pinService;

    private User testUser;
    private Card testCard;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(UserRole.USER);

        testAccount = new Account();
        testAccount.setStatus(AccountStatus.ACTIVE);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setAccount(testAccount);
        testCard.setStatus(CardStatus.ACTIVE);
    }

    @Test
    void setPinForCard_Success() {
        SetPinRequest request = new SetPinRequest("1234", "1234");
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(testCard));
        when(passwordEncoder.encode("1234")).thenReturn("hashed_pin");

        pinService.setPinForCard(request, 1L);

        assertEquals("hashed_pin", testCard.getPinHash());
        verify(cardRepository).save(testCard);
    }

    @Test
    void setPinForCard_BadRequest_Mismatch() {
        SetPinRequest request = new SetPinRequest("1234", "4321");
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(testCard));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> pinService.setPinForCard(request, 1L));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("confirmation does not match"));
    }

    @Test
    void changePinForCard_Success() {
        testCard.setPinHash("old_hash");
        ChangePinRequest request = new ChangePinRequest("5678", "1234");
        
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(testCard));
        when(passwordEncoder.matches("1234", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("5678")).thenReturn("new_hash");

        pinService.changePinForCard(request, 1L);

        assertEquals("new_hash", testCard.getPinHash());
        verify(rateLimitService).reset(1L, 1L);
        verify(cardRepository).save(testCard);
    }

    @Test
    void changePinForCard_Forbidden_InvalidOldPin() {
        testCard.setPinHash("old_hash");
        ChangePinRequest request = new ChangePinRequest("5678", "wrong");
        
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(testCard));
        when(passwordEncoder.matches("wrong", "old_hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> pinService.changePinForCard(request, 1L));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(rateLimitService).registerFailure(1L, 1L);
    }

    @Test
    void changePinForCard_TooManyRequests_Blocked() {
        testCard.setPinHash("old_hash");
        ChangePinRequest request = new ChangePinRequest("5678", "1234");
        
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(testCard));
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Blocked"))
                .when(rateLimitService).check(1L, 1L);

        assertThrows(ResponseStatusException.class, () -> pinService.changePinForCard(request, 1L));
    }
}
