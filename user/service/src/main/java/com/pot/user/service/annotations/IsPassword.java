package com.pot.user.service.annotations;

import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.annotations.validators.IsPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author: Pot
 * @created: 2025/3/26 22:44
 * @description: 密码注解校验
 */
@Documented
@Constraint(
        validatedBy = {IsPasswordValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsPassword {
    String message() default "Password is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String regex() default ValidationUtils.PASSWORD_REGEX;
}
