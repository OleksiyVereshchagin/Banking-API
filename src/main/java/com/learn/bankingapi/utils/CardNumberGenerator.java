package com.learn.bankingapi.utils;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating valid credit card numbers.
 *
 * Algorithm: Luhn algorithm (MOD 10) for check digit calculation.
 */
@Component
public class CardNumberGenerator {

    private static final String BIN = "444111";
    private static final int CARD_LENGTH = 16;

    public String generate() {
        StringBuilder number = new StringBuilder(BIN);

        int randomLength = CARD_LENGTH - BIN.length() - 1;

        for (int i = 0; i < randomLength; i++) {
            int digit = ThreadLocalRandom.current().nextInt(0, 10);
            number.append(digit);
        }

        int checkDigit = calculateLuhnDigit(number.toString());

        return number.append(checkDigit).toString();
    }

    private int calculateLuhnDigit(String number) {
        int sum = 0;
        boolean shouldDouble = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';

            if (shouldDouble) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }

            sum += digit;
            shouldDouble = !shouldDouble;
        }

        return (10 - (sum % 10)) % 10;
    }
}