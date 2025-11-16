package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: Pot
 * @created: 2025/11/16 22:48
 * @description: 密码验证器
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private String pattern;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(password)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidPassword(password)
                : ValidationUtils.isValidPassword(password, pattern);
    }
}
