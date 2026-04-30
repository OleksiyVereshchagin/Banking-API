package com.learn.bankingapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CardMaskerTest {

    private CardMasker cardMasker;

    @BeforeEach
    void setUp() {
        cardMasker = new CardMasker();
    }

    @Test
    void mask_ValidCardNumber_Success() {
        String cardNumber = "1234567812345678";
        String masked = cardMasker.mask(cardNumber);
        assertEquals("1234 **** **** 5678", masked);
    }

    @Test
    void mask_LongerCardNumber_Success() {
        String cardNumber = "1234567890123456789";
        String masked = cardMasker.mask(cardNumber);
        assertEquals("1234 **** **** 6789", masked);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "", "1"})
    void mask_InvalidLength_ThrowsException(String invalidNumber) {
        assertThrows(IllegalArgumentException.class, () -> cardMasker.mask(invalidNumber));
    }

    @Test
    void mask_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> cardMasker.mask(null));
    }
}
