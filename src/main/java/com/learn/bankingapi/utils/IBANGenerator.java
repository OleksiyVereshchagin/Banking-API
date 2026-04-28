package com.learn.bankingapi.utils;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating unique IBAN (International Bank Account Number) strings.
 * Follows the Ukrainian IBAN format (UA + 2 check digits + 6 digits MFO + 19 digits account number).
 *
 * Algorithm: ISO 7064 (MOD 97-10) for check digit calculation.
 */
@Component
public class IBANGenerator {

    private static final String COUNTRY_CODE = "UA";
    private static final String MFO = "300101";
    private static final String ACCOUNT_TYPE = "2620";

    public String generate() {
        String accountNumber = ACCOUNT_TYPE +
                String.format("%011d", ThreadLocalRandom.current().nextLong(0, 1_000_000_00000L));

        String checkString = MFO + accountNumber + convertCountryCode(COUNTRY_CODE) + "00";

        BigInteger bigInt = new BigInteger(checkString);
        int remainder = bigInt.mod(BigInteger.valueOf(97)).intValue();

        int checkDigits = 98 - remainder;

        return COUNTRY_CODE + String.format("%02d", checkDigits) + MFO + accountNumber;
    }

    private String convertCountryCode(String countryCode) {
        StringBuilder result = new StringBuilder();
        for (char ch : countryCode.toCharArray()) {
            result.append(Character.getNumericValue(ch));
        }
        return result.toString();
    }
}