package com.learn.bankingapi.utils;

import org.springframework.stereotype.Component;

/**
 * Utility for masking sensitive card information for display purposes.
 */
@Component
public class CardMasker {

    public String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            throw new IllegalArgumentException("Invalid card number for masking");
        }

        String first = cardNumber.substring(0, 4);
        String last = cardNumber.substring(cardNumber.length() - 4);

        return first + " **** **** " + last;
    }
}