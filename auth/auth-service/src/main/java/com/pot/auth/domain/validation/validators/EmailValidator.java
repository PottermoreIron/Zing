package com.pot.auth.domain.validation.validators;

import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: Pot
 * @created: 2025/11/16 22:30
 * @description: 邮箱验证器
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private String pattern;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.pattern = constraintAnnotation.pattern();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(email)) {
            return true;
        }
        return StringUtils.isBlank(pattern)
                ? ValidationUtils.isValidEmail(email)
                : ValidationUtils.isValidEmail(email, pattern);
    }
}
