package com.pot.user.service.annotations.validators;

import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.annotations.IsPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author: Pot
 * @created: 2025/3/26 22:45
 * @description: 密码校验
 */
public class IsPasswordValidator implements ConstraintValidator<IsPassword, String> {
    private String regex;

    @Override
    public void initialize(IsPassword constraintAnnotation) {
        this.regex = constraintAnnotation.regex();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return ValidationUtils.isValidPassword(s, regex);
    }
}
