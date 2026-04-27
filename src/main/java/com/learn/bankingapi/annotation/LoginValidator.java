package com.learn.bankingapi.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class LoginValidator implements ConstraintValidator<ValidLogin, String> {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return false;
        }

        return EMAIL.matcher(value).matches()
                || PHONE.matcher(value).matches();
    }
}