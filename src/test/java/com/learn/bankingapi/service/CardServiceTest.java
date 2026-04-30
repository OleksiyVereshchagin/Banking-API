package com.learn.bankingapi.service;

import com.learn.bankingapi.dto.request.card.CreateCardRequest;
import com.learn.bankingapi.dto.request.card.UpdateCardStatusRequest;
import com.learn.bankingapi.dto.response.card.CardResponse;
import com.learn.bankingapi.entity.Account;
import com.learn.bankingapi.entity.Card;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.enums.AccountStatus;
import com.learn.bankingapi.enums.CardStatus;
import com.learn.bankingapi.enums.CardType;
import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.mapper.CardMapper;
import com.learn.bankingapi.repository.AccountRepository;
import com.learn.bankingapi.repository.CardRepository;
import com.learn.bankingapi.utils.AccountAccessValidator;
import com.learn.bankingapi.utils.CardNumberGenerator;
import com.learn.bankingapi.utils.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AccountAccessValidator accountAccessValidator;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(UserRole.USER);

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void createCard_Success() {
        CreateCardRequest request = new CreateCardRequest(CardType.VIRTUAL);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));
        when(cardNumberGenerator.generate()).thenReturn("1234567812345678");
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMapper.toDto(any())).thenReturn(new CardResponse(1L, "1234 ****", LocalDate.now().plusYears(5), CardStatus.ACTIVE, CardType.VIRTUAL, true));

        CardResponse response = cardService.createCard(1L, request);

        assertNotNull(response);
        verify(cardRepository).save(any());
        verify(accountAccessValidator).validateCanOperate(testAccount);
    }

    @Test
    void getCardsByAccount_Success() {
        Card card = new Card();
        card.setAccount(testAccount);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(accountRepository.findAccountByIdAndUser(1L, testUser)).thenReturn(Optional.of(testAccount));
        when(cardRepository.findAllByAccount(testAccount)).thenReturn(List.of(card));
        when(cardMapper.toDto(card)).thenReturn(new CardResponse(1L, "1234 ****", LocalDate.now().plusYears(5), CardStatus.ACTIVE, CardType.VIRTUAL, true));

        List<CardResponse> responses = cardService.getCardsByAccount(1L);

        assertEquals(1, responses.size());
    }

    @Test
    void getCardDetails_Success() {
        Card card = new Card();
        card.setId(1L);
        card.setAccount(testAccount);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(new CardResponse(1L, "1234 ****", LocalDate.now().plusYears(5), CardStatus.ACTIVE, CardType.VIRTUAL, true));

        CardResponse response = cardService.getCardDetails(1L);

        assertNotNull(response);
    }

    @Test
    void updateCardStatus_User_Freeze_Success() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);
        card.setAccount(testAccount);

        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.FROZEN);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(new CardResponse(1L, "1234 ****", LocalDate.now().plusYears(5), CardStatus.FROZEN, CardType.VIRTUAL, true));

        CardResponse response = cardService.updateCardStatus(request, 1L);

        assertEquals(CardStatus.FROZEN, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void updateCardStatus_User_Forbidden_Block() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);
        card.setAccount(testAccount);

        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> cardService.updateCardStatus(request, 1L));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("USER can only freeze card"));
    }

    @Test
    void updateCardStatus_BadRequest_AlreadyClosed() {
        Card card = new Card();
        card.setStatus(CardStatus.CLOSED);
        card.setAccount(testAccount);

        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.FROZEN);
        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> cardService.updateCardStatus(request, 1L));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("card status is immutable"));
    }

    @Test
    void deleteCard_Success() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);
        card.setAccount(testAccount);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));

        cardService.deleteCard(1L);

        assertEquals(CardStatus.CLOSED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void deleteCard_BadRequest_AlreadyClosed() {
        Card card = new Card();
        card.setStatus(CardStatus.CLOSED);
        card.setAccount(testAccount);

        when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
        when(cardRepository.findByIdAndAccountUserId(1L, 1L)).thenReturn(Optional.of(card));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> cardService.deleteCard(1L));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Card is already closed"));
    }
}
