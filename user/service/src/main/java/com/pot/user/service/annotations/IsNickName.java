package com.pot.user.service.annotations;

import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.annotations.validators.IsNickNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author: Pot
 * @created: 2025/3/26 22:43
 * @description: 校验昵称注解
 */
@Documented
@Constraint(
        validatedBy = {IsNickNameValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsNickName {
    String message() default "Nickname is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String regex() default ValidationUtils.NICKNAME_REGEX;
}
