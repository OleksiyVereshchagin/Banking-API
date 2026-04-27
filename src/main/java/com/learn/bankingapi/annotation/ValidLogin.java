package com.learn.bankingapi.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LoginValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLogin {

    String message() default "Login must be valid email or phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
