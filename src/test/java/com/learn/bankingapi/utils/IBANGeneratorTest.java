package com.learn.bankingapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class IBANGeneratorTest {

    private IBANGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new IBANGenerator();
    }

    @Test
    void generate_ReturnsCorrectFormat() {
        String iban = generator.generate();
        assertNotNull(iban);
        assertEquals(29, iban.length());
        assertTrue(iban.startsWith("UA"), "Ukrainian IBAN should start with UA");
        assertTrue(iban.matches("UA\\d{27}"), "IBAN should contain UA followed by 27 digits");
    }

    @RepeatedTest(10)
    void generate_PassesValidation() {
        String iban = generator.generate();
        assertTrue(isValidIBAN(iban), "IBAN " + iban + " should be valid according to MOD 97-10");
    }

    private boolean isValidIBAN(String iban) {
        // 1. Move first 4 chars to end
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        
        // 2. Convert letters to digits (A=10, B=11, ..., Z=35)
        StringBuilder numeric = new StringBuilder();
        for (char ch : rearranged.toCharArray()) {
            if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else {
                numeric.append(Character.getNumericValue(ch));
            }
        }
        
        // 3. Mod 97
        BigInteger bigInt = new BigInteger(numeric.toString());
        return bigInt.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
