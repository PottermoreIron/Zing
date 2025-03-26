package com.pot.user.service.annotations.validators;

import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.annotations.IsNickName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author: Pot
 * @created: 2025/3/26 22:45
 * @description: 昵称校验
 */
public class IsNickNameValidator implements ConstraintValidator<IsNickName, String> {
    private String regex;

    @Override
    public void initialize(IsNickName constraintAnnotation) {
        this.regex = constraintAnnotation.regex();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return ValidationUtils.isValidNickname(s, regex);
    }
}
