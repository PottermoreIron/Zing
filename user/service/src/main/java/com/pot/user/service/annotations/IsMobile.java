package com.pot.user.service.annotations;

import com.pot.user.service.annotations.validators.IsMobileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author: Pot
 * @created: 2025/3/16 23:08
 * @description: 校验是否是手机号
 */
@Documented
@Constraint(
        validatedBy = {IsMobileValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsMobile {

    String message() default "Phone number is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
