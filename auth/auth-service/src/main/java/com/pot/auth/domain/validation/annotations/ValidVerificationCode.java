package com.pot.auth.domain.validation.annotations;

import com.pot.auth.domain.validation.validators.VerificationCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = VerificationCodeValidator.class)
public @interface ValidVerificationCode {
    String message() default "Invalid verification code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String pattern() default "";

}