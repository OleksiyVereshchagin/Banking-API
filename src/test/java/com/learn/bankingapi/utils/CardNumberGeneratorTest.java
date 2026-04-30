package com.learn.bankingapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardNumberGeneratorTest {

    private CardNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CardNumberGenerator();
    }

    @Test
    void generate_Returns16Digits() {
        String cardNumber = generator.generate();
        assertNotNull(cardNumber);
        assertEquals(16, cardNumber.length());
        assertTrue(cardNumber.matches("\\d{16}"), "Card number should contain 16 digits");
    }

    @Test
    void generate_StartsWithBIN() {
        String cardNumber = generator.generate();
        assertTrue(cardNumber.startsWith("444111"));
    }

    @RepeatedTest(10)
    void generate_PassesLuhnCheck() {
        String cardNumber = generator.generate();
        assertTrue(isValidLuhn(cardNumber), "Card number " + cardNumber + " should pass Luhn check");
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
