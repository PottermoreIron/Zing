package com.pot.auth.domain.validation.annotations;

import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author: Pot
 * @created: 2025/11/16 22:01
 * @description: 有效手机号验证
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidPhone {
    String message() default "Invalid phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String pattern() default "";
}
